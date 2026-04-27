package com.banking.system.domain.account.exception;
import com.banking.system.domain.account.AccountId;
import java.util.Currency;

/** Lançada quando a moeda do valor não corresponde à moeda da conta. */
public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(AccountId id, Currency esperada, Currency recebida) {
        super(String.format("Incompatibilidade de moeda na conta %s: esperada=%s, recebida=%s",
              id, esperada.getCurrencyCode(), recebida.getCurrencyCode()));
    }
}
