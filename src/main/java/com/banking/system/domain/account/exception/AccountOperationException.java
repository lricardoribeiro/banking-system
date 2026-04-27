package com.banking.system.domain.account.exception;
import com.banking.system.domain.account.AccountId;

/** Lançada quando uma operação inválida é tentada em uma conta. */
public class AccountOperationException extends RuntimeException {
    public AccountOperationException(AccountId id, String msg) {
        super("Conta " + id + ": " + msg);
    }
}
