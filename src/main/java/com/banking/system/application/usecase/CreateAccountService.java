package com.banking.system.application.usecase;

import com.banking.system.application.port.in.CreateAccountUseCase;
import com.banking.system.application.port.out.PublishDomainEventPort;
import com.banking.system.application.port.out.SaveAccountPort;
import com.banking.system.application.usecase.command.CreateAccountCommand;
import com.banking.system.application.usecase.result.AccountCreatedResult;
import com.banking.system.domain.account.Account;
import com.banking.system.infrastructure.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: criação de uma nova conta bancária. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateAccountService implements CreateAccountUseCase {

    private final SaveAccountPort saveAccountPort;
    private final PublishDomainEventPort publishDomainEventPort;

    @Override
    @Transactional
    public AccountCreatedResult createAccount(CreateAccountCommand command) {
        log.info("Criando conta para titular={} moeda={}", command.ownerId(), command.currency());

        Account account = Account.open(command.ownerId(), command.currency());
        Account salva   = saveAccountPort.saveAccount(account);

        // Publica os eventos após o commit bem-sucedido no banco
        salva.getDomainEvents().forEach(publishDomainEventPort::publish);
        salva.clearDomainEvents();

        log.info("Conta criada: id={}", salva.getId());
        return new AccountCreatedResult(salva.getId(), salva.getOwnerId(),
                                        salva.getCurrency(), salva.getCreatedAt());
    }
}
