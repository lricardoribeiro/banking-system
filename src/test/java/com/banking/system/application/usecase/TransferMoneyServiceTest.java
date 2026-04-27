package com.banking.system.application.usecase;

import com.banking.system.application.port.out.*;
import com.banking.system.application.usecase.command.TransferMoneyCommand;
import com.banking.system.application.usecase.result.TransferResult;
import com.banking.system.domain.account.Account;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.account.exception.AccountNotFoundException;
import com.banking.system.domain.account.exception.InsufficientFundsException;
import com.banking.system.domain.transfer.Transfer;
import com.banking.system.domain.transfer.TransferStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferMoneyService")
class TransferMoneyServiceTest {

    @Mock LoadAccountPort loadAccountPort;
    @Mock SaveAccountPort saveAccountPort;
    @Mock LoadLedgerEntriesPort loadLedgerEntriesPort;
    @Mock SaveLedgerEntryPort saveLedgerEntryPort;
    @Mock LoadTransferPort loadTransferPort;
    @Mock SaveTransferPort saveTransferPort;
    @Mock PublishDomainEventPort publishDomainEventPort;
    @Mock IdempotencyPort idempotencyPort;
    @Spy  io.micrometer.core.instrument.MeterRegistry meterRegistry = new SimpleMeterRegistry();

    TransferMoneyService service;

    private static final Currency BRL = Currency.getInstance("BRL");
    private AccountId sourceId;
    private AccountId targetId;
    private Account sourceAccount;
    private Account targetAccount;
    private final String idempotencyKey = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        service = new TransferMoneyService(
                loadAccountPort, saveAccountPort, loadLedgerEntriesPort,
                saveLedgerEntryPort, loadTransferPort, saveTransferPort,
                publishDomainEventPort, idempotencyPort, meterRegistry);

        // Cria contas com IDs onde source < target (lexicograficamente) para garantir ordem de lock previsível
        sourceId = AccountId.of("aaaaaaaa-0000-0000-0000-000000000001");
        targetId = AccountId.of("ffffffff-0000-0000-0000-000000000002");

        sourceAccount = Account.open("proprietario-A", BRL);
        targetAccount = Account.open("proprietario-B", BRL);
    }

    @Test
    @DisplayName("Deve transferir dinheiro com sucesso")
    void shouldTransferSuccessfully() {
        Money amount = Money.ofBRL("100.00");
        Money sourceBalance = Money.ofBRL("500.00");

        when(loadTransferPort.loadTransferByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());  // nenhuma transferência anterior
        when(loadAccountPort.loadAccountForUpdate(any()))
                .thenReturn(Optional.of(sourceAccount))
                .thenReturn(Optional.of(targetAccount));
        when(loadLedgerEntriesPort.calculateBalance(any()))
                .thenReturn(sourceBalance);
        when(saveAccountPort.saveAccount(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saveLedgerEntryPort.saveLedgerEntries(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saveTransferPort.saveTransfer(any())).thenAnswer(inv -> inv.getArgument(0));

        TransferResult result = service.transfer(new TransferMoneyCommand(
                sourceId, targetId, amount, idempotencyKey, "Transferência de teste"));

        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.idempotentResponse()).isFalse();
        verify(saveLedgerEntryPort).saveLedgerEntries(argThat(entries ->
                entries.size() == 2));   // DÉBITO + CRÉDITO
        verify(publishDomainEventPort, atLeastOnce()).publish(any());
        verify(idempotencyPort).store(eq(idempotencyKey), any());
    }

    @Test
    @DisplayName("Deve retornar resultado idempotente quando transferência já foi processada")
    void shouldReturnIdempotentResult() {
        Transfer existingTransfer = Transfer.initiate(sourceId, targetId,
                Money.ofBRL("100"), idempotencyKey);
        existingTransfer.complete();

        when(loadTransferPort.loadTransferByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingTransfer));

        TransferResult result = service.transfer(new TransferMoneyCommand(
                sourceId, targetId, Money.ofBRL("100"), idempotencyKey, null));

        assertThat(result.idempotentResponse()).isTrue();
        verifyNoInteractions(saveAccountPort, saveLedgerEntryPort);
    }

    @Test
    @DisplayName("Deve lançar InsufficientFundsException quando saldo for insuficiente")
    void shouldThrowOnInsufficientFunds() {
        when(loadTransferPort.loadTransferByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(loadAccountPort.loadAccountForUpdate(any()))
                .thenReturn(Optional.of(sourceAccount))
                .thenReturn(Optional.of(targetAccount));
        when(loadLedgerEntriesPort.calculateBalance(any()))
                .thenReturn(Money.ofBRL("50.00")); // saldo < valor solicitado

        assertThatThrownBy(() -> service.transfer(new TransferMoneyCommand(
                sourceId, targetId, Money.ofBRL("100.00"), idempotencyKey, null)))
                .isInstanceOf(InsufficientFundsException.class);

        verifyNoInteractions(saveTransferPort, saveLedgerEntryPort);
    }

    @Test
    @DisplayName("Deve lançar AccountNotFoundException quando conta de origem não for encontrada")
    void shouldThrowWhenSourceAccountMissing() {
        when(loadTransferPort.loadTransferByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(loadAccountPort.loadAccountForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(new TransferMoneyCommand(
                sourceId, targetId, Money.ofBRL("100"), idempotencyKey, null)))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
