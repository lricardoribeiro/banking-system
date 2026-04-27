package com.banking.system.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload de requisição para criação de conta bancária.
 * currency deve ser um código ISO 4217 válido de 3 letras (ex: "BRL", "USD").
 */
public record CreateAccountRequest(
        @NotBlank @Size(max = 100)
        String ownerId,

        @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "Deve ser um código de moeda ISO 4217 válido")
        String currency
) {}
