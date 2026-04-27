package com.banking.system.adapter.in.web.dto;

import java.time.Instant;

/**
 * Resposta retornada após uma transferência.
 * idempotentResponse=true indica que a transferência já havia sido processada
 * anteriormente com a mesma chave de idempotência (resposta em cache).
 */
public record TransferResponse(
        String transferId,
        String status,
        boolean idempotentResponse, // true = resultado do cache de idempotência
        Instant processedAt
) {}
