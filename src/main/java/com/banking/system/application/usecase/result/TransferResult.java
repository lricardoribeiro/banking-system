package com.banking.system.application.usecase.result;

import com.banking.system.domain.transfer.TransferId;
import com.banking.system.domain.transfer.TransferStatus;
import java.time.Instant;

/** Resultado retornado após a execução de uma transferência. */
public record TransferResult(
        TransferId transferId,
        TransferStatus status,
        boolean idempotentResponse,   // true se o resultado veio do cache de idempotência
        Instant processedAt
) {
    public static TransferResult success(TransferId id) {
        return new TransferResult(id, TransferStatus.COMPLETED, false, Instant.now());
    }
    public static TransferResult idempotent(TransferId id) {
        return new TransferResult(id, TransferStatus.COMPLETED, true, Instant.now());
    }
    public static TransferResult failed(TransferId id) {
        return new TransferResult(id, TransferStatus.FAILED, false, Instant.now());
    }
}
