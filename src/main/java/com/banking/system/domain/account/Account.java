package com.banking.system.domain.account;

import com.banking.system.domain.account.event.AccountCreatedEvent;
import com.banking.system.domain.account.event.MoneyCreditedEvent;
import com.banking.system.domain.account.event.MoneyDebitedEvent;
import com.banking.system.domain.account.exception.*;
import com.banking.system.domain.shared.AggregateRoot;
import com.banking.system.domain.transfer.TransferId;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

/**
 * Aggregate Root: Conta Bancária.
 *
 * === Decisões de Design ===
 *
 * 1. SALDO NÃO É ARMAZENADO NA CONTA.
 *    O ledger (LedgerEntry) é a fonte de verdade do saldo.
 *    Saldo = SUM(entradas CREDIT) - SUM(entradas DEBIT) para este accountId.
 *    Isso garante auditabilidade total e permite consultas de saldo em ponto-no-tempo
 *    ("qual era o saldo em 15/03/2024?"), evitando divergências causadas por bugs.
 *    Para desempenho em contas de alto volume, um padrão de snapshot pode ser
 *    adicionado como camada CQRS sem alterar este modelo central.
 *
 * 2. OPTIMISTIC LOCKING via campo `version`.
 *    Cenário: dois requests concorrentes leem Account(version=5).
 *    Ambos tentam UPDATE WHERE version=5. Apenas um vence; o outro recebe
 *    OptimisticLockException -> mapeado para HTTP 409 -> cliente reenvia.
 *    Isso evita SELECT FOR UPDATE (pessimista) que mantém locks no banco
 *    e reduz o throughput. Adequado para contas com concorrência moderada.
 *    Para contas muito quentes (ex: merchant recebendo milhares de pagamentos/seg),
 *    considere uma fila por conta ou saldo baseado em CRDT.
 *
 * 3. MODELO DE DOMÍNIO RICO (não anêmico).
 *    As regras de negócio vivem dentro do aggregate, não espalhadas em serviços.
 *    O aggregate é o único responsável por garantir seus invariantes.
 *
 * 4. PADRÃO FACTORY METHOD.
 *    Account.open() é a única forma de criar uma nova conta (registra eventos).
 *    Account.reconstitute() é usado pela camada de persistência (sem eventos).
 */
public class Account extends AggregateRoot<AccountId> {

    private final AccountId id;
    private final String ownerId;
    private AccountStatus status;
    private final Currency currency;

    /**
     * Versão para controle de locking otimista.
     * Incrementada pelo JPA a cada UPDATE.
     * Se duas transações concorrentes leram version=N e tentam UPDATE,
     * apenas uma vence — a outra recebe OptimisticLockException.
     */
    private long version;

    private final Instant createdAt;
    private Instant updatedAt;


    private Account(AccountId id, String ownerId, Currency currency) {
        this.id = id; this.ownerId = ownerId; this.currency = currency;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = this.updatedAt = Instant.now();
        this.version = 0L;
    }

    private Account(AccountId id, String ownerId, AccountStatus status,
                    Currency currency, long version, Instant createdAt, Instant updatedAt) {
        this.id = id; this.ownerId = ownerId; this.status = status;
        this.currency = currency; this.version = version;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }


    /**
     * Abre uma nova conta bancária para o titular informado.
     * Registra AccountCreatedEvent como efeito colateral.
     */
    public static Account open(String ownerId, Currency currency) {
        Objects.requireNonNull(ownerId, "ownerId não pode ser nulo");
        Objects.requireNonNull(currency, "currency não pode ser nulo");
        Account a = new Account(AccountId.generate(), ownerId, currency);
        a.registerEvent(AccountCreatedEvent.of(a.id, ownerId, currency));
        return a;
    }

    /**
     * Reconstitui uma Conta a partir da persistência.
     * NÃO registra eventos de domínio (já ocorreram quando os fatos aconteceram).
     * Usado exclusivamente pelo adaptador de persistência.
     */
    public static Account reconstitute(AccountId id, String ownerId, AccountStatus status,
                                       Currency currency, long version,
                                       Instant createdAt, Instant updatedAt) {
        return new Account(id, ownerId, status, currency, version, createdAt, updatedAt);
    }


    /**
     * Registra um débito nesta conta.
     *
     * Nota: a validação de saldo suficiente é responsabilidade do TransferMoneyService,
     * que consulta o ledger. Este aggregate valida apenas invariantes de estado e moeda.
     */
    public void debit(Money amount, TransferId transferId) {
        assertAtiva(); assertMoeda(amount); assertPositivo(amount);
        updatedAt = Instant.now();
        registerEvent(MoneyDebitedEvent.of(id, amount, transferId, updatedAt));
    }

    /**
     * Registra um crédito nesta conta.
     */
    public void credit(Money amount, TransferId transferId) {
        assertAtiva(); assertMoeda(amount); assertPositivo(amount);
        updatedAt = Instant.now();
        registerEvent(MoneyCreditedEvent.of(id, amount, transferId, updatedAt));
    }

    /**
     * Bloqueia a conta, impedindo novas transações.
     * Contas bloqueadas podem ser desbloqueadas (diferente de ENCERRADA, que é terminal).
     */
    public void block() {
        if (status == AccountStatus.CLOSED)
            throw new AccountOperationException(id, "Não é possível bloquear uma conta encerrada");
        if (status == AccountStatus.BLOCKED)
            throw new AccountOperationException(id, "A conta já está bloqueada");
        status = AccountStatus.BLOCKED; updatedAt = Instant.now();
    }

    /**
     * Desbloqueia uma conta previamente bloqueada.
     */
    public void unblock() {
        if (status != AccountStatus.BLOCKED)
            throw new AccountOperationException(id, "Somente contas BLOQUEADAS podem ser desbloqueadas");
        status = AccountStatus.ACTIVE; updatedAt = Instant.now();
    }

    /**
     * Encerra a conta permanentemente.
     * O saldo atual (obtido do ledger pelo caso de uso) deve ser zero.
     */
    public void close(Money currentBalance) {
        if (status == AccountStatus.CLOSED)
            throw new AccountOperationException(id, "A conta já está encerrada");
        if (!currentBalance.isZero())
            throw new AccountOperationException(id, "Não é possível encerrar conta com saldo: " + currentBalance);
        status = AccountStatus.CLOSED; updatedAt = Instant.now();
    }


    private void assertAtiva() {
        if (status != AccountStatus.ACTIVE) throw new AccountNotActiveException(id, status);
    }
    private void assertMoeda(Money m) {
        if (!m.currency().equals(currency)) throw new CurrencyMismatchException(id, currency, m.currency());
    }
    private void assertPositivo(Money m) {
        if (!m.isPositive()) throw new AccountOperationException(id, "O valor da transação deve ser positivo");
    }


    @Override public AccountId getId() { return id; }
    public String getOwnerId()         { return ownerId; }
    public AccountStatus getStatus()   { return status; }
    public Currency getCurrency()      { return currency; }
    public long getVersion()           { return version; }
    public Instant getCreatedAt()      { return createdAt; }
    public Instant getUpdatedAt()      { return updatedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        return (o instanceof Account a) && Objects.equals(id, a.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return "Conta{id=" + id + ", titular=" + ownerId + ", status=" + status + "}";
    }
}
