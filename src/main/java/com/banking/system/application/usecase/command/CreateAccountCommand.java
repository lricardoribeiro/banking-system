package com.banking.system.application.usecase.command;

import java.util.Currency;
import java.util.Objects;

/**
 * Comando para criação de uma nova conta bancária.
 * Comandos são value objects imutáveis que transportam entrada validada ao caso de uso.
 */
public record CreateAccountCommand(String ownerId, Currency currency) {
    public CreateAccountCommand {
        Objects.requireNonNull(ownerId, "ownerId não pode ser nulo");
        if (ownerId.isBlank()) throw new IllegalArgumentException("ownerId não pode estar em branco");
        Objects.requireNonNull(currency, "currency não pode ser nula");
    }
    public static CreateAccountCommand of(String ownerId, String codigoMoeda) {
        return new CreateAccountCommand(ownerId, Currency.getInstance(codigoMoeda));
    }
}
