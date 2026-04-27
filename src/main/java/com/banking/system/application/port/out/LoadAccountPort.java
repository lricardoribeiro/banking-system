package com.banking.system.application.port.out;

import com.banking.system.domain.account.Account;
import com.banking.system.domain.account.AccountId;
import java.util.Optional;

/** Porta Secundária: carrega uma Conta da persistência. */
public interface LoadAccountPort {
    Optional<Account> loadAccount(AccountId accountId);

    /**
     * Carrega a conta com lock PESSIMISTIC_WRITE para casos de uso em que
     * o custo de retentativa do locking otimista é inaceitável (ex: operações em lote).
     */
    Optional<Account> loadAccountForUpdate(AccountId accountId);
}
