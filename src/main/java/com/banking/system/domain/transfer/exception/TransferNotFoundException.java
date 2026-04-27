package com.banking.system.domain.transfer.exception;
import com.banking.system.domain.transfer.TransferId;

/** Lançada quando uma transferência não é encontrada na persistência. */
public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException(TransferId id) {
        super("Transferência não encontrada: " + id);
    }
}
