package com.banking.system.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Resposta de erro padronizada para toda a API.
 * traceId corresponde ao X-Correlation-ID para rastreabilidade nos logs.
 */
public record ErrorResponse(
        String error,           // código de erro de alto nível (ex: "SALDO_INSUFICIENTE")
        String message,         // mensagem legível para o desenvolvedor
        List<String> details,   // erros de validação por campo
        String traceId,         // correlação com logs (X-Correlation-ID)
        Instant timestamp
) {
    public ErrorResponse(String error, String message, String traceId) {
        this(error, message, List.of(), traceId, Instant.now());
    }
}
