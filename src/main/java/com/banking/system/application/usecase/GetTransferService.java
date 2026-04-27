package com.banking.system.application.usecase;

import com.banking.system.application.port.in.GetTransferUseCase;
import com.banking.system.application.port.out.LoadTransferPort;
import com.banking.system.domain.transfer.Transfer;
import com.banking.system.domain.transfer.TransferId;
import com.banking.system.domain.transfer.exception.TransferNotFoundException;
import com.banking.system.infrastructure.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: consulta de uma transferência por ID. */
@UseCase
@RequiredArgsConstructor
public class GetTransferService implements GetTransferUseCase {

    private final LoadTransferPort loadTransferPort;

    @Override
    @Transactional(readOnly = true)
    public Transfer getTransfer(TransferId transferId) {
        return loadTransferPort.loadTransfer(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }
}
