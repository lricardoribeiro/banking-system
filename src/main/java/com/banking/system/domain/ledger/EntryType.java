package com.banking.system.domain.ledger;

/**
 * Tipos de lançamento na contabilidade de dupla entrada (Double-Entry Bookkeeping).
 *
 * Na contabilidade de dupla entrada, toda transação financeira afeta
 * pelo menos duas contas: um DÉBITO e um CRÉDITO.
 * A soma de todos os débitos DEVE ser igual à soma de todos os créditos por transação.
 * Este é o invariante fundamental: SUM(lançamentos por transferência) = 0
 * quando representado como: CRÉDITO = positivo, DÉBITO = negativo.
 *
 * Exemplo — transferência de R$ 100 da Conta A para a Conta B:
 *   - Conta A: DÉBITO  R$ 100  (dinheiro sai)
 *   - Conta B: CRÉDITO R$ 100  (dinheiro entra)
 *   Soma = 0 ✓  (balanceado)
 */
public enum EntryType {
    DEBIT,   // dinheiro sai da conta
    CREDIT   // dinheiro entra na conta
}
