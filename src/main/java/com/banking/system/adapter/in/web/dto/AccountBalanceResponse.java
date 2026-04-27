package com.banking.system.adapter.in.web.dto;

import java.math.BigDecimal;

/**
 * Resposta de consulta de saldo.
 * O saldo é calculado em tempo real a partir do livro-razão (ledger),
 * nunca armazenado diretamente na conta (fonte única de verdade = lançamentos).
 */
public record AccountBalanceResponse(
        String accountId,
        BigDecimal balance,
        String currency
) {}
