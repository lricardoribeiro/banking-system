package com.banking.system.domain.transfer.exception;

/** Lançada quando se tenta processar uma transferência com chave de idempotência duplicada. */
public class DuplicateTransferException extends RuntimeException {
    public DuplicateTransferException(String idempotencyKey) {
        super("Transferência com chave de idempotência já processada: " + idempotencyKey);
    }
}
