package com.banking.system.domain.transfer;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.shared.AggregateRoot;
import com.banking.system.domain.transfer.event.TransferCompletedEvent;
import com.banking.system.domain.transfer.event.TransferFailedEvent;
import com.banking.system.domain.transfer.event.TransferInitiatedEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root: Transferência Bancária.
 *
 * Representa uma solicitação de movimentação de dinheiro entre duas contas.
 * A Transferência atua como coordenadora de saga: rastreia o estado da operação
 * e conduz o processo de negócio até a conclusão ou compensação.
 *
 * === Chave de Idempotência ===
 *   Cada transferência carrega uma idempotencyKey fornecida pelo cliente.
 *   Antes de criar uma Transferência, o serviço de aplicação verifica essa chave.
 *   Se a chave já existe no armazenamento de idempotência, o resultado anterior
 *   é retornado sem re-executar a transferência. Isso torna a API
 *   segura para retentativas do cliente (ex: timeouts de rede).
 *
 * === Padrão Saga (Transação Local) ===
 *   Para transferências no mesmo serviço, usamos uma transação ACID local:
 *     BEGIN TX -> debit(contaOrigem) + credit(contaDestino)
 *              + salvarLançamentos + salvarTransferência -> COMMIT
 *   Esta é a abordagem mais simples e consistente quando ambas as contas
 *   estão no mesmo serviço/banco de dados.
 *
 *   Para transferências distribuídas (entre serviços), use Saga por Coreografia:
 *     1. TransferService publica TransferInitiatedEvent
 *     2. AccountService(A) debita e publica AccountDebitedEvent
 *     3. AccountService(B) credita e publica AccountCreditedEvent
 *     4. TransferService escuta ambos e marca como CONCLUÍDA
 *   Com transações compensatórias (estorno) se o passo 3 falhar.
 *
 * === Trade-off do Teorema CAP ===
 *   Transferências financeiras exigem CONSISTÊNCIA sobre DISPONIBILIDADE.
 *   Escolhemos CP: se o banco estiver particionado, falhamos a requisição (503)
 *   em vez de arriscar double-spending ou perda de dinheiro.
 *   A chave de idempotência permite retentativas seguras pelo cliente.
 */
public class Transfer extends AggregateRoot<TransferId> {

    private final TransferId id;
    private final AccountId sourceAccountId;
    private final AccountId targetAccountId;
    private final Money amount;
    private final String idempotencyKey;
    private TransferStatus status;
    private String failureReason;
    private final Instant createdAt;
    private Instant updatedAt;

    private Transfer(TransferId id, AccountId source, AccountId target,
                     Money amount, String idempotencyKey) {
        this.id = id;
        this.sourceAccountId = source;
        this.targetAccountId = target;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = TransferStatus.PENDING;
        this.createdAt = this.updatedAt = Instant.now();
    }

    private Transfer(TransferId id, AccountId source, AccountId target,
                     Money amount, String idempotencyKey, TransferStatus status,
                     String failureReason, Instant createdAt, Instant updatedAt) {
        this.id = id; this.sourceAccountId = source; this.targetAccountId = target;
        this.amount = amount; this.idempotencyKey = idempotencyKey; this.status = status;
        this.failureReason = failureReason; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }


    /** Inicia uma nova transferência. Registra TransferInitiatedEvent. */
    public static Transfer initiate(AccountId source, AccountId target,
                                    Money amount, String idempotencyKey) {
        Objects.requireNonNull(source, "contaOrigem não pode ser nula");
        Objects.requireNonNull(target, "contaDestino não pode ser nula");
        Objects.requireNonNull(amount, "valor não pode ser nulo");
        Objects.requireNonNull(idempotencyKey, "chaveIdempotencia não pode ser nula");
        if (source.equals(target))
            throw new IllegalArgumentException("Conta origem e destino devem ser diferentes");
        if (!amount.isPositive())
            throw new IllegalArgumentException("O valor da transferência deve ser positivo");

        Transfer t = new Transfer(TransferId.generate(), source, target, amount, idempotencyKey);
        t.registerEvent(TransferInitiatedEvent.of(t.id, source, target, amount, idempotencyKey));
        return t;
    }

    /** Reconstitui uma Transferência a partir da persistência. */
    public static Transfer reconstitute(TransferId id, AccountId source, AccountId target,
                                        Money amount, String idempotencyKey, TransferStatus status,
                                        String failureReason, Instant createdAt, Instant updatedAt) {
        return new Transfer(id, source, target, amount, idempotencyKey,
                            status, failureReason, createdAt, updatedAt);
    }


    /** Conclui a transferência com sucesso. Registra TransferCompletedEvent. */
    public void complete() {
        assertPendente("concluir");
        status = TransferStatus.COMPLETED;
        updatedAt = Instant.now();
        registerEvent(TransferCompletedEvent.of(id, sourceAccountId, targetAccountId, amount));
    }

    /** Marca a transferência como falha. Registra TransferFailedEvent. */
    public void fail(String reason) {
        assertPendente("falhar");
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
        registerEvent(TransferFailedEvent.of(id, reason));
    }

    /** Inicia o processo de compensação (rollback de saga). */
    public void startCompensation() {
        if (status != TransferStatus.COMPLETED)
            throw new IllegalStateException("Somente transferências CONCLUÍDAS podem ser compensadas");
        status = TransferStatus.COMPENSATING;
        updatedAt = Instant.now();
    }

    /** Finaliza o processo de compensação. */
    public void markCompensated() {
        if (status != TransferStatus.COMPENSATING)
            throw new IllegalStateException("A transferência não está em estado COMPENSANDO");
        status = TransferStatus.COMPENSATED;
        updatedAt = Instant.now();
    }

    private void assertPendente(String operacao) {
        if (status != TransferStatus.PENDING)
            throw new IllegalStateException("Não é possível " + operacao +
                " transferência no status: " + status + " (id=" + id + ")");
    }


    @Override public TransferId getId()       { return id; }
    public AccountId getSourceAccountId()     { return sourceAccountId; }
    public AccountId getTargetAccountId()     { return targetAccountId; }
    public Money getAmount()                  { return amount; }
    public String getIdempotencyKey()         { return idempotencyKey; }
    public TransferStatus getStatus()         { return status; }
    public String getFailureReason()          { return failureReason; }
    public Instant getCreatedAt()             { return createdAt; }
    public Instant getUpdatedAt()             { return updatedAt; }
    public boolean isPending()                { return status == TransferStatus.PENDING; }
    public boolean isCompleted()              { return status == TransferStatus.COMPLETED; }
    public boolean isFailed()                 { return status == TransferStatus.FAILED; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        return (o instanceof Transfer t) && Objects.equals(id, t.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
