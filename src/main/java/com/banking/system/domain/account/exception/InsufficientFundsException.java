package com.banking.system.domain.account.exception;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;

/** Lançada quando o saldo da conta é insuficiente para a operação solicitada. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(AccountId id, Money balance, Money requested) {
        super(String.format("Saldo insuficiente na conta %s: saldo=%s, solicitado=%s",
              id, balance, requested));
    }
}
