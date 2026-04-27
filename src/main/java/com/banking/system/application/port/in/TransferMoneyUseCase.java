package com.banking.system.application.port.in;

import com.banking.system.application.usecase.command.TransferMoneyCommand;
import com.banking.system.application.usecase.result.TransferResult;

/**
 * Porta Primária (driving): transferência de dinheiro entre contas.
 * Este é o caso de uso mais crítico do sistema.
 */
public interface TransferMoneyUseCase {
    TransferResult transfer(TransferMoneyCommand command);
}
