package com.banking.system.domain.account;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object: identidade fortemente tipada para o aggregate Account.
 * Usar um tipo dedicado (não UUID cru) evita confusão acidental
 * entre AccountId, TransferId, LedgerEntryId em assinaturas de métodos.
 */
public record AccountId(UUID value) {
    public AccountId { Objects.requireNonNull(value, "AccountId não pode ser nulo"); }
    public static AccountId generate() { return new AccountId(UUID.randomUUID()); }
    public static AccountId of(String id) { return new AccountId(UUID.fromString(id)); }
    public static AccountId of(UUID id)   { return new AccountId(id); }
    @Override public String toString()    { return value.toString(); }
}
