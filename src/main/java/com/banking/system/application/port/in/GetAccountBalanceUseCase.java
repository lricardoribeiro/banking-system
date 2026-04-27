package com.banking.system.application.port.in;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;

/**
 * Porta Primária (driving): consulta do saldo atual de uma conta.
 * O saldo é calculado a partir do ledger (fonte de verdade), não de um campo armazenado.
 */
public interface GetAccountBalanceUseCase {
    Money getBalance(AccountId accountId);
}
