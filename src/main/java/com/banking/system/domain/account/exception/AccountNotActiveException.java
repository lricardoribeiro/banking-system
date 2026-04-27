package com.banking.system.domain.account.exception;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.AccountStatus;

/** Lançada quando uma operação é tentada em uma conta que não está ATIVA. */
public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(AccountId id, AccountStatus status) {
        super("A conta " + id + " não está ATIVA. Status atual: " + status);
    }
}
