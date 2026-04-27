package com.banking.system.application.usecase;

import com.banking.system.application.port.in.GetAccountBalanceUseCase;
import com.banking.system.application.port.out.LoadAccountPort;
import com.banking.system.application.port.out.LoadLedgerEntriesPort;
import com.banking.system.domain.account.Account;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.account.exception.AccountNotFoundException;
import com.banking.system.infrastructure.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: consulta do saldo atual de uma conta, calculado a partir do ledger. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetAccountBalanceService implements GetAccountBalanceUseCase {

    private final LoadAccountPort loadAccountPort;
    private final LoadLedgerEntriesPort loadLedgerEntriesPort;

    @Override
    @Transactional(readOnly = true)
    public Money getBalance(AccountId accountId) {
        Account account = loadAccountPort.loadAccount(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Saldo calculado do ledger (fonte autoritativa)
        Money saldo = loadLedgerEntriesPort.calculateBalance(account.getId());
        log.debug("Saldo da conta {}: {}", accountId, saldo);
        return saldo;
    }
}
