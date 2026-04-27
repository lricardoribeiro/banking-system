package com.banking.system.domain.transfer;

import java.util.Objects;
import java.util.UUID;

/** Value Object: identidade fortemente tipada para o aggregate Transfer. */
public record TransferId(UUID value) {
    public TransferId { Objects.requireNonNull(value, "TransferId não pode ser nulo"); }
    public static TransferId generate() { return new TransferId(UUID.randomUUID()); }
    public static TransferId of(String id) { return new TransferId(UUID.fromString(id)); }
    public static TransferId of(UUID id)   { return new TransferId(id); }
    @Override public String toString()     { return value.toString(); }
}
