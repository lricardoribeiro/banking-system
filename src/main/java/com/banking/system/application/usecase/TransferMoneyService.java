package com.banking.system.application.usecase;

import com.banking.system.application.port.in.TransferMoneyUseCase;
import com.banking.system.application.port.out.*;
import com.banking.system.application.usecase.command.TransferMoneyCommand;
import com.banking.system.application.usecase.result.TransferResult;
import com.banking.system.domain.account.Account;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.account.exception.AccountNotFoundException;
import com.banking.system.domain.account.exception.InsufficientFundsException;
import com.banking.system.domain.ledger.LedgerEntry;
import com.banking.system.domain.transfer.Transfer;
import com.banking.system.infrastructure.annotation.UseCase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * TransferMoneyService – Serviço de Aplicação que implementa o Caso de Uso de Transferência.
 *
 * === Fronteira Transacional ===
 *   @Transactional com Isolation.REPEATABLE_READ.
 *
 *   Por que REPEATABLE_READ?
 *   - READ_COMMITTED (padrão no PostgreSQL): uma transação concorrente poderia INSERT
 *     um novo lançamento no ledger entre nossa leitura de saldo e a escrita do débito,
 *     causando leitura de saldo desatualizado e possível descoberto.
 *   - REPEATABLE_READ evita phantom reads de lançamentos do ledger dentro da nossa transação.
 *   - Não usamos SERIALIZABLE para evitar o custo de verificações completas de serialização;
 *     REPEATABLE_READ + locking otimista na Conta oferece segurança suficiente.
 *
 * === Fluxo de Locking Otimista ===
 *   1. Carrega Conta (lê version=N)
 *   2. Calcula saldo do ledger
 *   3. Valida regras de negócio
 *   4. Salva Conta (UPDATE WHERE version=N)
 *      -> Se outra transação atualizou a conta concorrentemente, version mudou
 *        -> OptimisticLockException -> HTTP 409 -> cliente retenta com mesma chave de idempotência
 *      -> Na retentativa, o check de idempotência pode encontrar que a tentativa anterior teve sucesso
 *
 * === Mecanismo de Idempotência ===
 *   - Cliente fornece idempotencyKey (UUID) por operação lógica.
 *   - Antes de executar, verificamos se a chave existe na tabela idempotency_keys.
 *   - Se sim: retorna resultado cacheado (sem dupla execução).
 *   - Se não: executa a transferência e armazena a chave + resultado atomicamente.
 *   - A constraint UNIQUE na tabela garante check-and-store atômico e livre de race condition.
 *
 * === Invariante do Ledger de Dupla Entrada ===
 *   Para cada transferência, criamos exatamente:
 *     - 1 lançamento DÉBITO na conta origem
 *     - 1 lançamento CRÉDITO na conta destino
 *   Ambos usam o mesmo transferId, permitindo reconciliação.
 *   Validamos SUM(DEBIT) == SUM(CREDIT) antes do commit.
 *
 * === Ordenação de Locks ===
 *   Para evitar deadlocks em transferências concorrentes entre as mesmas duas contas
 *   (A->B e B->A simultâneos), adquirimos locks sempre em ORDEM DETERMINÍSTICA
 *   baseada na comparação de strings do ID da conta. Isso garante que A->B e B->A
 *   ambos adquiram lock na conta de menor ID primeiro, evitando espera circular.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class TransferMoneyService implements TransferMoneyUseCase {

    private final LoadAccountPort     loadAccountPort;
    private final SaveAccountPort     saveAccountPort;
    private final LoadLedgerEntriesPort loadLedgerEntriesPort;
    private final SaveLedgerEntryPort saveLedgerEntryPort;
    private final LoadTransferPort    loadTransferPort;
    private final SaveTransferPort    saveTransferPort;
    private final PublishDomainEventPort publishDomainEventPort;
    private final IdempotencyPort     idempotencyPort;
    private final MeterRegistry       meterRegistry;

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransferResult transfer(TransferMoneyCommand command) {
        log.info("Transferência iniciada: de={} para={} valor={} chaveIdempotencia={}",
                command.sourceAccountId(), command.targetAccountId(),
                command.amount(), command.idempotencyKey());

        // Passo 1 – Verificação de idempotência
        // Se uma chamada anterior com esta chave teve sucesso, retorna imediatamente.
        Optional<Transfer> transferenciaExistente =
                loadTransferPort.loadTransferByIdempotencyKey(command.idempotencyKey());
        if (transferenciaExistente.isPresent()) {
            log.info("Resposta idempotente para chave={}, transferId={}",
                     command.idempotencyKey(), transferenciaExistente.get().getId());
            meterRegistry.counter("transfers.idempotent").increment();
            return TransferResult.idempotent(transferenciaExistente.get().getId());
        }

        Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            // Passo 2 – Carrega contas com ordenação determinística de locks
            // Previne deadlock A->B / B->A adquirindo locks sempre na mesma ordem.
            AccountId primeiroLock, segundoLock;
            if (command.sourceAccountId().toString()
                        .compareTo(command.targetAccountId().toString()) < 0) {
                primeiroLock  = command.sourceAccountId();
                segundoLock = command.targetAccountId();
            } else {
                primeiroLock  = command.targetAccountId();
                segundoLock = command.sourceAccountId();
            }

            Account primeiro  = loadAccountPort.loadAccountForUpdate(primeiroLock)
                    .orElseThrow(() -> new AccountNotFoundException(primeiroLock));
            Account segundo = loadAccountPort.loadAccountForUpdate(segundoLock)
                    .orElseThrow(() -> new AccountNotFoundException(segundoLock));

            Account contaOrigem  = primeiroLock.equals(command.sourceAccountId())
                    ? primeiro : segundo;
            Account contaDestino = primeiroLock.equals(command.targetAccountId())
                    ? primeiro : segundo;

            // Passo 3 – Calcula saldo do ledger (REPEATABLE_READ protege esta leitura)
            Money saldoOrigem = loadLedgerEntriesPort.calculateBalance(contaOrigem.getId());
            log.debug("Saldo da conta origem: {}", saldoOrigem);

            // Passo 4 – Regra de negócio: saldo suficiente
            if (saldoOrigem.isLessThan(command.amount())) {
                throw new InsufficientFundsException(contaOrigem.getId(),
                                                     saldoOrigem, command.amount());
            }

            // Passo 5 – Cria o aggregate Transfer
            Transfer transferencia = Transfer.initiate(
                    command.sourceAccountId(), command.targetAccountId(),
                    command.amount(), command.idempotencyKey());

            // Passo 6 – Aplica operações de domínio nas contas
            contaOrigem.debit(command.amount(), transferencia.getId());
            contaDestino.credit(command.amount(), transferencia.getId());

            // Passo 7 – Cria lançamentos de dupla entrada no ledger
            String descricao = command.description() != null ? command.description()
                        : "Transferência " + transferencia.getId();
            LedgerEntry lancamentoDebito  = LedgerEntry.createDebit(
                    contaOrigem.getId(), transferencia.getId(), command.amount(), descricao);
            LedgerEntry lancamentoCredito = LedgerEntry.createCredit(
                    contaDestino.getId(), transferencia.getId(), command.amount(), descricao);

            // Passo 8 – Valida invariante de dupla entrada
            validarDuplaEntrada(List.of(lancamentoDebito, lancamentoCredito));

            // Passo 9 – Conclui o aggregate Transfer
            transferencia.complete();

            // Passo 10 – Persiste atomicamente (mesma transação @Transactional)
            saveAccountPort.saveAccount(contaOrigem);
            saveAccountPort.saveAccount(contaDestino);
            saveLedgerEntryPort.saveLedgerEntries(List.of(lancamentoDebito, lancamentoCredito));
            saveTransferPort.saveTransfer(transferencia);

            // Passo 11 – Armazena chave de idempotência
            idempotencyPort.store(command.idempotencyKey(), transferencia.getId().toString());

            // Passo 12 – Publica eventos de domínio (idealmente via Outbox Pattern)
            publicarEventos(contaOrigem, contaDestino, transferencia);

            timerSample.stop(meterRegistry.timer("transfers.duration", "status", "success"));
            meterRegistry.counter("transfers.success").increment();

            log.info("Transferência concluída: id={} de={} para={} valor={}",
                    transferencia.getId(), command.sourceAccountId(),
                    command.targetAccountId(), command.amount());

            return TransferResult.success(transferencia.getId());

        } catch (InsufficientFundsException | AccountNotFoundException e) {
            timerSample.stop(meterRegistry.timer("transfers.duration", "status", "failed"));
            meterRegistry.counter("transfers.failed").increment();
            log.warn("Transferência falhou: {}", e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            // Violação da constraint de chave de idempotência – request duplicado concorrente
            timerSample.stop(meterRegistry.timer("transfers.duration", "status", "idempotent"));
            meterRegistry.counter("transfers.idempotent").increment();
            log.warn("Chave de idempotência duplicada detectada: {}", command.idempotencyKey());
            Optional<Transfer> existente =
                    loadTransferPort.loadTransferByIdempotencyKey(command.idempotencyKey());
            return existente.map(t -> TransferResult.idempotent(t.getId()))
                    .orElseThrow(() -> new IllegalStateException(
                            "Violação de constraint mas transferência não encontrada", e));
        }
    }

    /**
     * Valida o invariante fundamental da contabilidade de dupla entrada:
     * SUM(débitos) == SUM(créditos) para a transação.
     * Serve como rede de segurança – se falhar, há um erro de programação.
     */
    private void validarDuplaEntrada(List<LedgerEntry> lancamentos) {
        var moeda = lancamentos.get(0).getAmount().currency();
        var totalDebitos  = lancamentos.stream()
                .filter(LedgerEntry::isDebit)
                .map(LedgerEntry::getAmount)
                .reduce(Money.zero(moeda), Money::add);
        var totalCreditos = lancamentos.stream()
                .filter(LedgerEntry::isCredit)
                .map(LedgerEntry::getAmount)
                .reduce(Money.zero(moeda), Money::add);

        if (!totalDebitos.equals(totalCreditos)) {
            throw new IllegalStateException(String.format(
                "Invariante de dupla entrada violado! débitos=%s, créditos=%s",
                totalDebitos, totalCreditos));
        }
    }

    private void publicarEventos(Account origem, Account destino, Transfer transferencia) {
        origem.getDomainEvents().forEach(publishDomainEventPort::publish);
        origem.clearDomainEvents();
        destino.getDomainEvents().forEach(publishDomainEventPort::publish);
        destino.clearDomainEvents();
        transferencia.getDomainEvents().forEach(publishDomainEventPort::publish);
        transferencia.clearDomainEvents();
    }
}
