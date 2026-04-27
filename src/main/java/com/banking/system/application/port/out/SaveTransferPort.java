package com.banking.system.application.port.out;

import com.banking.system.domain.transfer.Transfer;

/** Porta Secundária: persiste uma Transferência (criar ou atualizar). */
public interface SaveTransferPort {
    Transfer saveTransfer(Transfer transfer);
}
