package com.banking.system.domain.transfer;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.transfer.event.TransferCompletedEvent;
import com.banking.system.domain.transfer.event.TransferInitiatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Aggregate Transfer")
class TransferTest {

    private final AccountId origem  = AccountId.generate();
    private final AccountId destino = AccountId.generate();
    private final Money valor       = Money.ofBRL("500.00");
    private final String chaveidem  = UUID.randomUUID().toString();

    @Test
    @DisplayName("Deve iniciar transferência no estado PENDENTE")
    void deveIniciarNoPendente() {
        Transfer t = Transfer.initiate(origem, destino, valor, chaveidem);

        assertThat(t.getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(t.getSourceAccountId()).isEqualTo(origem);
        assertThat(t.getTargetAccountId()).isEqualTo(destino);
        assertThat(t.getAmount()).isEqualTo(valor);
        assertThat(t.getIdempotencyKey()).isEqualTo(chaveidem);
    }

    @Test
    @DisplayName("Deve registrar TransferInitiatedEvent ao iniciar")
    void deveRegistrarEventoIniciado() {
        Transfer t = Transfer.initiate(origem, destino, valor, chaveidem);

        assertThat(t.getDomainEvents()).hasSize(1);
        assertThat(t.getDomainEvents().get(0)).isInstanceOf(TransferInitiatedEvent.class);
    }

    @Test
    @DisplayName("Deve concluir uma transferência PENDENTE")
    void deveConcluir() {
        Transfer t = Transfer.initiate(origem, destino, valor, chaveidem);
        t.clearDomainEvents();
        t.complete();

        assertThat(t.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(t.getDomainEvents()).hasSize(1);
        assertThat(t.getDomainEvents().get(0)).isInstanceOf(TransferCompletedEvent.class);
    }

    @Test
    @DisplayName("Deve marcar transferência PENDENTE como falha")
    void deveMarcarComoFalha() {
        Transfer t = Transfer.initiate(origem, destino, valor, chaveidem);
        t.fail("Saldo insuficiente");

        assertThat(t.getStatus()).isEqualTo(TransferStatus.FAILED);
        assertThat(t.getFailureReason()).isEqualTo("Saldo insuficiente");
    }

    @Test
    @DisplayName("Não deve concluir uma transferência já FALHOU")
    void naoDeveConcluirTransferenciaFalha() {
        Transfer t = Transfer.initiate(origem, destino, valor, chaveidem);
        t.fail("motivo");
        assertThatThrownBy(t::complete).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção quando origem igual ao destino")
    void deveLancarExcecaoComMesmaConta() {
        assertThatThrownBy(() -> Transfer.initiate(origem, origem, valor, chaveidem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diferentes");
    }

    @Test
    @DisplayName("Deve lançar exceção com valor zero")
    void deveLancarExcecaoComValorZero() {
        assertThatThrownBy(() -> Transfer.initiate(origem, destino, Money.zero(valor.currency()), chaveidem))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }
}
