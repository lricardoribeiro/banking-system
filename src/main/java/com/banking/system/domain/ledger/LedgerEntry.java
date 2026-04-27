package com.banking.system.domain.ledger;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.transfer.TransferId;

import java.time.Instant;
import java.util.Objects;

/**
 * Entrada do Ledger – unidade atômica da contabilidade de dupla entrada.
 *
 * === Contabilidade de Dupla Entrada ===
 *   Todo evento monetário cria um par BALANCEADO de lançamentos:
 *     (DÉBITO na conta origem) + (CRÉDITO na conta destino)
 *   O invariante: para todo TransferId,
 *     SUM(valores DEBIT) == SUM(valores CREDIT)
 *
 *   Saldo de uma conta = SUM(créditos) - SUM(débitos) para aquele accountId.
 *
 * === Imutabilidade ===
 *   Entradas do ledger são IMUTÁVEIS após a criação. Elas são a trilha de auditoria.
 *   Nunca é emitido UPDATE ou DELETE na tabela do ledger.
 *   Correções são feitas via lançamentos compensatórios (novas linhas, não modificações).
 *
 * === Não é um Aggregate Root ===
 *   LedgerEntry é uma entidade de domínio no contexto do aggregate Transfer,
 *   mas acessada/consultada independentemente para cálculo de saldo.
 *   Não possui eventos de domínio próprios; o aggregate Transfer emite
 *   TransferCompletedEvent sinalizando que as entradas do ledger existem.
 */
public class LedgerEntry {

    private final LedgerEntryId id;
    private final TransferId transferId;   // vincula o par débito+crédito
    private final AccountId accountId;
    private final EntryType type;
    private final Money amount;
    private final Instant createdAt;
    private final String description;

    private LedgerEntry(LedgerEntryId id, TransferId transferId, AccountId accountId,
                        EntryType type, Money amount, String description, Instant createdAt) {
        this.id = id;
        this.transferId = transferId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }


    /** Cria um lançamento de DÉBITO (dinheiro sai da conta). */
    public static LedgerEntry createDebit(AccountId accountId, TransferId transferId,
                                          Money amount, String description) {
        Objects.requireNonNull(accountId, "accountId não pode ser nulo");
        Objects.requireNonNull(transferId, "transferId não pode ser nulo");
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        if (!amount.isPositive())
            throw new IllegalArgumentException("O valor do lançamento deve ser positivo");
        return new LedgerEntry(LedgerEntryId.generate(), transferId, accountId,
                               EntryType.DEBIT, amount, description, Instant.now());
    }

    /** Cria um lançamento de CRÉDITO (dinheiro entra na conta). */
    public static LedgerEntry createCredit(AccountId accountId, TransferId transferId,
                                           Money amount, String description) {
        Objects.requireNonNull(accountId, "accountId não pode ser nulo");
        Objects.requireNonNull(transferId, "transferId não pode ser nulo");
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        if (!amount.isPositive())
            throw new IllegalArgumentException("O valor do lançamento deve ser positivo");
        return new LedgerEntry(LedgerEntryId.generate(), transferId, accountId,
                               EntryType.CREDIT, amount, description, Instant.now());
    }
    /**
     * Reconstitui um LedgerEntry a partir da persistência.
     * Preserva ID e createdAt originais — NÃO gera novos valores.
     * Usado exclusivamente pelo adaptador de persistência.
     */
    public static LedgerEntry reconstitute(LedgerEntryId id, TransferId transferId,
                                           AccountId accountId, EntryType type,
                                           Money amount, String description, Instant createdAt) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Objects.requireNonNull(transferId, "transferId não pode ser nulo");
        Objects.requireNonNull(accountId, "accountId não pode ser nulo");
        Objects.requireNonNull(type, "type não pode ser nulo");
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        Objects.requireNonNull(createdAt, "createdAt não pode ser nulo");
        return new LedgerEntry(id, transferId, accountId, type, amount, description, createdAt);
    }



    public LedgerEntryId getId()      { return id; }
    public TransferId getTransferId() { return transferId; }
    public AccountId getAccountId()   { return accountId; }
    public EntryType getType()        { return type; }
    public Money getAmount()          { return amount; }
    public Instant getCreatedAt()     { return createdAt; }
    public String getDescription()    { return description; }

    public boolean isDebit()  { return type == EntryType.DEBIT; }
    public boolean isCredit() { return type == EntryType.CREDIT; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        return (o instanceof LedgerEntry e) && Objects.equals(id, e.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return "LedgerEntry{id=" + id + ", tipo=" + type + ", valor=" + amount +
               ", conta=" + accountId + ", transferencia=" + transferId + "}";
    }
}
