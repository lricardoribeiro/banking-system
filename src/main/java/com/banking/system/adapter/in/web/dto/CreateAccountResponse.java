package com.banking.system.adapter.in.web.dto;

import java.time.Instant;

/** Resposta retornada após a criação bem-sucedida de uma conta. */
public record CreateAccountResponse(
        String accountId,
        String ownerId,
        String currency,
        Instant createdAt
) {}
