package com.banking.system.application.usecase.command;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import java.util.Objects;

/**
 * Comando para iniciar uma transferência de dinheiro.
 *
 * idempotencyKey: UUID gerado pelo cliente que garante execução única.
 * O cliente DEVE gerar um novo UUID para cada transferência logicamente distinta,
 * e REUTILIZAR o mesmo UUID ao retentar a mesma transferência após um timeout.
 */
public record TransferMoneyCommand(
        AccountId sourceAccountId,
        AccountId targetAccountId,
        Money amount,
        String idempotencyKey,
        String description
) {
    public TransferMoneyCommand {
        Objects.requireNonNull(sourceAccountId, "contaOrigem não pode ser nula");
        Objects.requireNonNull(targetAccountId, "contaDestino não pode ser nula");
        Objects.requireNonNull(amount, "valor não pode ser nulo");
        Objects.requireNonNull(idempotencyKey, "chaveIdempotencia não pode ser nula");
        if (idempotencyKey.isBlank())
            throw new IllegalArgumentException("chaveIdempotencia não pode estar em branco");
        if (sourceAccountId.equals(targetAccountId))
            throw new IllegalArgumentException("Conta origem e destino devem ser diferentes");
        if (!amount.isPositive())
            throw new IllegalArgumentException("O valor da transferência deve ser positivo");
    }
}
