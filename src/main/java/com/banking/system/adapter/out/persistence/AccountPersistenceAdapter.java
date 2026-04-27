package com.banking.system.adapter.out.persistence;

import com.banking.system.adapter.out.persistence.entity.AccountJpaEntity;
import com.banking.system.adapter.out.persistence.repository.AccountJpaRepository;
import com.banking.system.application.port.out.LoadAccountPort;
import com.banking.system.application.port.out.SaveAccountPort;
import com.banking.system.domain.account.Account;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.AccountStatus;
import com.banking.system.infrastructure.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Currency;
import java.util.Optional;

/** Adaptador de persistência para o aggregate Account. */
@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class AccountPersistenceAdapter implements LoadAccountPort, SaveAccountPort {

    private final AccountJpaRepository accountRepository;

    @Override
    public Optional<Account> loadAccount(AccountId accountId) {
        return accountRepository.findById(accountId.value()).map(this::paraDominio);
    }

    @Override
    public Optional<Account> loadAccountForUpdate(AccountId accountId) {
        return accountRepository.findByIdForUpdate(accountId.value()).map(this::paraDominio);
    }

    @Override
    public Account saveAccount(Account account) {
        AccountJpaEntity entidade = paraEntidade(account);
        AccountJpaEntity salva    = accountRepository.save(entidade);
        return paraDominio(salva);
    }


    private Account paraDominio(AccountJpaEntity e) {
        return Account.reconstitute(
                AccountId.of(e.getId()),
                e.getOwnerId(),
                AccountStatus.valueOf(e.getStatus()),
                Currency.getInstance(e.getCurrency()),
                e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    private AccountJpaEntity paraEntidade(Account a) {
        return AccountJpaEntity.builder()
                .id(a.getId().value())
                .ownerId(a.getOwnerId())
                .status(a.getStatus().name())
                .currency(a.getCurrency().getCurrencyCode())
                .version(a.getVersion())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
