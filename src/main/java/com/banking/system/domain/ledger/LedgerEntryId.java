package com.banking.system.domain.ledger;
import java.util.Objects;
import java.util.UUID;

/** Value Object: identidade fortemente tipada para entradas do ledger. */
public record LedgerEntryId(UUID value) {
    public LedgerEntryId { Objects.requireNonNull(value, "LedgerEntryId não pode ser nulo"); }
    public static LedgerEntryId generate() { return new LedgerEntryId(UUID.randomUUID()); }
    public static LedgerEntryId of(UUID id) { return new LedgerEntryId(id); }
    @Override public String toString() { return value.toString(); }
}
