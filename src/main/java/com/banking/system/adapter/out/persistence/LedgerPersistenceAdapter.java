package com.banking.system.adapter.out.persistence;

import com.banking.system.adapter.out.persistence.entity.LedgerEntryJpaEntity;
import com.banking.system.adapter.out.persistence.repository.LedgerEntryJpaRepository;
import com.banking.system.application.port.out.LoadLedgerEntriesPort;
import com.banking.system.application.port.out.SaveLedgerEntryPort;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.ledger.EntryType;
import com.banking.system.domain.ledger.LedgerEntry;
import com.banking.system.domain.ledger.LedgerEntryId;
import com.banking.system.domain.transfer.TransferId;
import com.banking.system.infrastructure.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

@PersistenceAdapter
@RequiredArgsConstructor
public class LedgerPersistenceAdapter implements LoadLedgerEntriesPort, SaveLedgerEntryPort {

    private final LedgerEntryJpaRepository ledgerRepository;

    @Override
    public List<LedgerEntry> loadEntriesByAccount(AccountId accountId) {
        return ledgerRepository.findByAccountIdOrderByCreatedAtAsc(accountId.value())
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<LedgerEntry> loadEntriesByTransfer(TransferId transferId) {
        return ledgerRepository.findByTransferId(transferId.value())
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Money calculateBalance(AccountId accountId) {
        BigDecimal balance = ledgerRepository.calculateBalance(accountId.value());
        // Determina a moeda a partir da conta – na prática, todos os lançamentos compartilham a mesma moeda.
        // É necessário ao menos um lançamento para determinar a moeda; para contas sem movimentação, retorna BRL zero.
        List<LedgerEntryJpaEntity> entries =
                ledgerRepository.findByAccountIdOrderByCreatedAtAsc(accountId.value());
        if (entries.isEmpty()) {
            // Nenhuma transação ainda – saldo é zero. Moeda desconhecida aqui; usa BRL como padrão.
            // Em produção, Account armazena a moeda e ela seria passada como parâmetro.
            return Money.ofBRL(BigDecimal.ZERO);
        }
        Currency currency = Currency.getInstance(entries.get(0).getCurrency());
        return Money.of(balance != null ? balance : BigDecimal.ZERO, currency);
    }

    @Override
    public LedgerEntry saveLedgerEntry(LedgerEntry entry) {
        LedgerEntryJpaEntity saved = ledgerRepository.save(toEntity(entry));
        return toDomain(saved);
    }

    @Override
    public List<LedgerEntry> saveLedgerEntries(List<LedgerEntry> entries) {
        List<LedgerEntryJpaEntity> entities = entries.stream()
                .map(this::toEntity).collect(Collectors.toList());
        return ledgerRepository.saveAll(entities).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    private LedgerEntry toDomain(LedgerEntryJpaEntity e) {
        EntryType type = EntryType.valueOf(e.getEntryType());
        Currency currency = Currency.getInstance(e.getCurrency());
        Money amount = Money.of(e.getAmount(), currency);
        return LedgerEntry.reconstitute(
                LedgerEntryId.of(e.getId()),
                TransferId.of(e.getTransferId()),
                AccountId.of(e.getAccountId()),
                type, amount, e.getDescription(),
                e.getCreatedAt());
    }

    private LedgerEntryJpaEntity toEntity(LedgerEntry e) {
        return LedgerEntryJpaEntity.builder()
                .id(e.getId().value())
                .transferId(e.getTransferId().value())
                .accountId(e.getAccountId().value())
                .entryType(e.getType().name())
                .amount(e.getAmount().amount())
                .currency(e.getAmount().currency().getCurrencyCode())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
