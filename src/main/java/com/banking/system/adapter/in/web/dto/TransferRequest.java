package com.banking.system.adapter.in.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Payload de requisição para transferência de dinheiro.
 * A idempotencyKey deve ser um UUID v4 gerado pelo cliente para
 * garantir segurança em retentativas sem duplo processamento.
 */
public record TransferRequest(
        @NotBlank String sourceAccountId,
        @NotBlank String targetAccountId,

        @NotNull @DecimalMin(value = "0.01", message = "O valor mínimo é 0,01")
        @Digits(integer = 15, fraction = 2, message = "Máximo de 15 dígitos inteiros e 2 casas decimais")
        BigDecimal amount,

        @NotBlank @Pattern(regexp = "[A-Z]{3}")
        String currency,

        @NotBlank @Size(min = 36, max = 64, message = "A chave de idempotência deve ter entre 36 e 64 caracteres")
        String idempotencyKey,

        @Size(max = 255)
        String description
) {}
