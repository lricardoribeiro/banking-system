package com.banking.system.application.port.out;

import com.banking.system.domain.account.Account;

/** Porta Secundária: persiste uma Conta (criar ou atualizar). */
public interface SaveAccountPort {
    Account saveAccount(Account account);
}
