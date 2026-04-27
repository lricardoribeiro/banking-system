package com.banking.system.application.port.in;

import com.banking.system.application.usecase.command.CreateAccountCommand;
import com.banking.system.application.usecase.result.AccountCreatedResult;

/**
 * Porta Primária (driving): criação de conta bancária.
 * Implementada por CreateAccountService; chamada pelo AccountController.
 */
public interface CreateAccountUseCase {
    AccountCreatedResult createAccount(CreateAccountCommand command);
}
