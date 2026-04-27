package com.banking.system.domain.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object representando um valor monetário com moeda específica.
 *
 * === Por que BigDecimal e não double? ===
 *   double usa ponto flutuante binário IEEE 754. 0.1 + 0.2 = 0.30000000000000004.
 *   Em operações bancárias de alto volume isso gera discrepâncias reais. BigDecimal é exato.
 *
 * === Por que HALF_EVEN (Arredondamento Bancário)? ===
 *   HALF_UP sempre arredonda 0.5 para cima, acumulando viés ao longo de milhões de transações.
 *   HALF_EVEN arredonda para o dígito par mais próximo, distribuindo os erros de arredondamento
 *   simetricamente – padrão em sistemas financeiros (modo padrão do IEEE 754).
 *
 * === Imutabilidade ===
 *   Todas as operações aritméticas retornam NOVAS instâncias de Money.
 *   Os valores são normalizados para 2 casas decimais na construção para garantir
 *   que equals() seja simétrico: Money.of("1.0") == Money.of("1.00").
 *
 * === Valores negativos ===
 *   Money é sempre não-negativo. A direcionalidade (débito vs crédito) é
 *   expressa pelo EntryType do LedgerEntry, não por sinal negativo.
 */
public record Money(BigDecimal amount, Currency currency) {

    /** Escala monetária padrão (2 casas decimais). */
    public static final int SCALE = 2;

    /**
     * HALF_EVEN (Arredondamento Bancário) distribui os erros de arredondamento
     * de forma mais uniforme que HALF_UP em grandes conjuntos de cálculos.
     */
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "O valor não pode ser nulo");
        Objects.requireNonNull(currency, "A moeda não pode ser nula");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Money não pode ser negativo: " + amount);
        // Normaliza a escala para evitar assimetria em equals() (ex: 1.0 != 1.00)
        amount = amount.setScale(SCALE, ROUNDING_MODE);
    }


    public static Money of(BigDecimal amount, Currency currency) { return new Money(amount, currency); }
    public static Money of(String amount, String code) { return new Money(new BigDecimal(amount), Currency.getInstance(code)); }
    public static Money ofBRL(BigDecimal amount)  { return new Money(amount, Currency.getInstance("BRL")); }
    public static Money ofBRL(String amount)      { return new Money(new BigDecimal(amount), Currency.getInstance("BRL")); }
    public static Money zero(Currency currency)   { return new Money(BigDecimal.ZERO, currency); }


    public Money add(Money o)      { mesmaMoeda(o); return new Money(amount.add(o.amount), currency); }
    public Money subtract(Money o) {
        mesmaMoeda(o);
        BigDecimal r = amount.subtract(o.amount);
        if (r.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Subtração resultaria em valor negativo: " + this + " - " + o);
        return new Money(r, currency);
    }


    public boolean isGreaterThan(Money o)        { mesmaMoeda(o); return amount.compareTo(o.amount) > 0; }
    public boolean isGreaterThanOrEqual(Money o) { mesmaMoeda(o); return amount.compareTo(o.amount) >= 0; }
    public boolean isLessThan(Money o)           { mesmaMoeda(o); return amount.compareTo(o.amount) < 0; }
    public boolean isZero()     { return amount.compareTo(BigDecimal.ZERO) == 0; }
    public boolean isPositive() { return amount.compareTo(BigDecimal.ZERO) > 0; }

    private void mesmaMoeda(Money o) {
        if (!currency.equals(o.currency))
            throw new IllegalArgumentException("Moedas incompatíveis: " + currency + " vs " + o.currency);
    }

    @Override public String toString() { return currency.getCurrencyCode() + " " + amount.toPlainString(); }
}
