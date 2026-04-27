package com.banking.system.domain.account;

import com.banking.system.domain.account.exception.*;
import com.banking.system.domain.account.event.*;
import com.banking.system.domain.transfer.TransferId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Aggregate Account")
class AccountTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Nested
    @DisplayName("Criação de conta")
    class Criacao {

        @Test
        @DisplayName("Deve criar conta ATIVA com dados corretos")
        void deveCriarContaAtiva() {
            Account conta = Account.open("titular-123", BRL);

            assertThat(conta.getOwnerId()).isEqualTo("titular-123");
            assertThat(conta.getCurrency()).isEqualTo(BRL);
            assertThat(conta.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(conta.getId()).isNotNull();
            assertThat(conta.getVersion()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Deve registrar AccountCreatedEvent ao abrir")
        void deveRegistrarEventoDeDominio() {
            Account conta = Account.open("titular-123", BRL);

            assertThat(conta.getDomainEvents()).hasSize(1);
            assertThat(conta.getDomainEvents().get(0)).isInstanceOf(AccountCreatedEvent.class);
            AccountCreatedEvent evento = (AccountCreatedEvent) conta.getDomainEvents().get(0);
            assertThat(evento.ownerId()).isEqualTo("titular-123");
        }

        @Test
        @DisplayName("Deve lançar exceção quando ownerId for nulo")
        void deveLancarExcecaoQuandoTitularNulo() {
            assertThatThrownBy(() -> Account.open(null, BRL))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção quando moeda for nula")
        void deveLancarExcecaoQuandoMoedaNula() {
            assertThatThrownBy(() -> Account.open("titular", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Operação de débito")
    class Debito {

        @Test
        @DisplayName("Deve registrar MoneyDebitedEvent")
        void deveRegistrarEventoDebito() {
            Account conta = Account.open("titular", BRL);
            conta.clearDomainEvents();
            Money valor = Money.ofBRL("100.00");
            TransferId tid = TransferId.generate();

            conta.debit(valor, tid);

            assertThat(conta.getDomainEvents()).hasSize(1);
            MoneyDebitedEvent evento = (MoneyDebitedEvent) conta.getDomainEvents().get(0);
            assertThat(evento.amount()).isEqualTo(valor);
            assertThat(evento.transferId()).isEqualTo(tid);
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta está BLOQUEADA")
        void deveLancarExcecaoQuandoBloqueada() {
            Account conta = Account.open("titular", BRL);
            conta.block();

            assertThatThrownBy(() -> conta.debit(Money.ofBRL("10"), TransferId.generate()))
                    .isInstanceOf(AccountNotActiveException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção em incompatibilidade de moeda")
        void deveLancarExcecaoEmIncompatibilidadeMoeda() {
            Account conta = Account.open("titular", BRL);
            Money valorUSD = Money.of("100", "USD");

            assertThatThrownBy(() -> conta.debit(valorUSD, TransferId.generate()))
                    .isInstanceOf(CurrencyMismatchException.class);
        }

        @Test
        @DisplayName("Deve lançar exceção com valor zero")
        void deveLancarExcecaoComValorZero() {
            Account conta = Account.open("titular", BRL);
            assertThatThrownBy(() -> conta.debit(Money.zero(BRL), TransferId.generate()))
                    .isInstanceOf(AccountOperationException.class);
        }
    }

    @Nested
    @DisplayName("Ciclo de vida da conta")
    class CicloDeVida {

        @Test
        @DisplayName("Deve bloquear uma conta ATIVA")
        void deveBloqueiarContaAtiva() {
            Account conta = Account.open("titular", BRL);
            conta.block();
            assertThat(conta.getStatus()).isEqualTo(AccountStatus.BLOCKED);
        }

        @Test
        @DisplayName("Deve desbloquear uma conta BLOQUEADA")
        void deveDesbloquearContaBloqueada() {
            Account conta = Account.open("titular", BRL);
            conta.block();
            conta.unblock();
            assertThat(conta.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Deve encerrar conta com saldo zero")
        void deveEncerrarComSaldoZero() {
            Account conta = Account.open("titular", BRL);
            conta.close(Money.zero(BRL));
            assertThat(conta.getStatus()).isEqualTo(AccountStatus.CLOSED);
        }

        @Test
        @DisplayName("Não deve encerrar conta com saldo positivo")
        void naoDeveEncerrarComSaldo() {
            Account conta = Account.open("titular", BRL);
            assertThatThrownBy(() -> conta.close(Money.ofBRL("100")))
                    .isInstanceOf(AccountOperationException.class)
                    .hasMessageContaining("saldo");
        }

        @Test
        @DisplayName("Não deve bloquear uma conta já ENCERRADA")
        void naoDeveBloquearContaEncerrada() {
            Account conta = Account.open("titular", BRL);
            conta.close(Money.zero(BRL));
            assertThatThrownBy(conta::block)
                    .isInstanceOf(AccountOperationException.class);
        }
    }
}
