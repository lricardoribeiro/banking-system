package com.banking.system.application.usecase.result;

import com.banking.system.domain.account.AccountId;
import java.time.Instant;
import java.util.Currency;

/** Resultado retornado após a criação bem-sucedida de uma conta bancária. */
public record AccountCreatedResult(
        AccountId accountId, String ownerId, Currency currency, Instant createdAt
) {}
