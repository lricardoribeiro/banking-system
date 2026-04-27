package com.banking.system.application.port.out;

import com.banking.system.domain.transfer.Transfer;
import com.banking.system.domain.transfer.TransferId;
import java.util.Optional;

/** Porta Secundária: carrega uma Transferência da persistência. */
public interface LoadTransferPort {
    Optional<Transfer> loadTransfer(TransferId transferId);
    Optional<Transfer> loadTransferByIdempotencyKey(String idempotencyKey);
}
