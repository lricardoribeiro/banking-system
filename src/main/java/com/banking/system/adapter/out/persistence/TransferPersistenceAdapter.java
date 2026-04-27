package com.banking.system.adapter.out.persistence;

import com.banking.system.adapter.out.persistence.entity.TransferJpaEntity;
import com.banking.system.adapter.out.persistence.repository.TransferJpaRepository;
import com.banking.system.application.port.out.LoadTransferPort;
import com.banking.system.application.port.out.SaveTransferPort;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.transfer.Transfer;
import com.banking.system.domain.transfer.TransferId;
import com.banking.system.domain.transfer.TransferStatus;
import com.banking.system.infrastructure.annotation.PersistenceAdapter;
import lombok.RequiredArgsConstructor;

import java.util.Currency;
import java.util.Optional;

/** Adaptador de persistência para o aggregate Transfer. */
@PersistenceAdapter
@RequiredArgsConstructor
public class TransferPersistenceAdapter implements LoadTransferPort, SaveTransferPort {

    private final TransferJpaRepository transferRepository;

    @Override
    public Optional<Transfer> loadTransfer(TransferId transferId) {
        return transferRepository.findById(transferId.value()).map(this::toDomain);
    }

    @Override
    public Optional<Transfer> loadTransferByIdempotencyKey(String idempotencyKey) {
        return transferRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public Transfer saveTransfer(Transfer transfer) {
        TransferJpaEntity saved = transferRepository.save(toEntity(transfer));
        return toDomain(saved);
    }

    private Transfer toDomain(TransferJpaEntity e) {
        return Transfer.reconstitute(
                TransferId.of(e.getId()),
                AccountId.of(e.getSourceAccountId()),
                AccountId.of(e.getTargetAccountId()),
                Money.of(e.getAmount(), Currency.getInstance(e.getCurrency())),
                e.getIdempotencyKey(),
                TransferStatus.valueOf(e.getStatus()),
                e.getFailureReason(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    private TransferJpaEntity toEntity(Transfer t) {
        return TransferJpaEntity.builder()
                .id(t.getId().value())
                .sourceAccountId(t.getSourceAccountId().value())
                .targetAccountId(t.getTargetAccountId().value())
                .amount(t.getAmount().amount())
                .currency(t.getAmount().currency().getCurrencyCode())
                .idempotencyKey(t.getIdempotencyKey())
                .status(t.getStatus().name())
                .failureReason(t.getFailureReason())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
