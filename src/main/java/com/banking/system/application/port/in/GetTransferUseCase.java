package com.banking.system.application.port.in;

import com.banking.system.domain.transfer.Transfer;
import com.banking.system.domain.transfer.TransferId;

/** Porta Primária (driving): consulta de uma transferência por ID. */
public interface GetTransferUseCase {
    Transfer getTransfer(TransferId transferId);
}
