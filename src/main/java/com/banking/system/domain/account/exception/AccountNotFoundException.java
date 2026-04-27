package com.banking.system.domain.account.exception;
import com.banking.system.domain.account.AccountId;

/** Lançada quando uma conta não é encontrada na persistência. */
public class AccountNotFoundException extends RuntimeException {
    private final AccountId accountId;
    public AccountNotFoundException(AccountId id) {
        super("Conta não encontrada: " + id);
        this.accountId = id;
    }
    public AccountId getAccountId() { return accountId; }
}
