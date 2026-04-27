package com.banking.system.domain.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Value Object Money")
class MoneyTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    @DisplayName("Deve normalizar escala para 2 casas decimais")
    void deveNormalizarEscala() {
        Money m1 = Money.ofBRL("100");
        Money m2 = Money.ofBRL("100.0");
        Money m3 = Money.ofBRL("100.00");
        assertThat(m1).isEqualTo(m2).isEqualTo(m3);
    }

    @Test
    @DisplayName("Deve somar dois valores Money")
    void deveSomar() {
        Money resultado = Money.ofBRL("100.50").add(Money.ofBRL("50.50"));
        assertThat(resultado.amount()).isEqualByComparingTo("151.00");
    }

    @Test
    @DisplayName("Deve subtrair valor menor do maior")
    void deveSubtrair() {
        Money resultado = Money.ofBRL("200.00").subtract(Money.ofBRL("50.00"));
        assertThat(resultado.amount()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Deve aplicar arredondamento HALF_EVEN (Arredondamento Bancário)")
    void deveAplicarArredondamentoBancario() {
        // 2.125 -> arredonda para 2.12 (HALF_EVEN: 2 é par, arredonda para baixo)
        Money m1 = Money.of(new BigDecimal("2.125"), BRL);
        assertThat(m1.amount()).isEqualByComparingTo("2.12");

        // 2.135 -> arredonda para 2.14 (HALF_EVEN: 4 é par, arredonda para cima)
        Money m2 = Money.of(new BigDecimal("2.135"), BRL);
        assertThat(m2.amount()).isEqualByComparingTo("2.14");
    }

    @Test
    @DisplayName("Deve lançar exceção para valor negativo")
    void deveLancarExcecaoParaNegativo() {
        assertThatThrownBy(() -> Money.ofBRL("-1.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("Deve lançar exceção em incompatibilidade de moeda na soma")
    void deveLancarExcecaoEmIncompatibilidadeMoedaNaSoma() {
        Money brl = Money.ofBRL("100");
        Money usd = Money.of("100", "USD");
        assertThatThrownBy(() -> brl.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incompatíveis");
    }

    @Test
    @DisplayName("Deve lançar exceção em subtração que resultaria em negativo")
    void deveLancarExcecaoEmSubtracaoNegativa() {
        assertThatThrownBy(() -> Money.ofBRL("50").subtract(Money.ofBRL("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({ "100,50,true", "50,100,false", "100,100,false" })
    @DisplayName("isGreaterThan deve comparar corretamente")
    void deveCompararCorretamente(String a, String b, boolean esperado) {
        assertThat(Money.ofBRL(a).isGreaterThan(Money.ofBRL(b))).isEqualTo(esperado);
    }
}
