package com.banking.system.domain.transfer.event;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.shared.DomainEvent;
import com.banking.system.domain.transfer.TransferId;
import java.time.Instant;
import java.util.UUID;

/** Evento de domínio disparado quando uma transferência é iniciada. */
public record TransferInitiatedEvent(
        UUID eventId, Instant occurredAt,
        TransferId transferId, AccountId sourceAccountId,
        AccountId targetAccountId, Money amount, String idempotencyKey
) implements DomainEvent {
    public static TransferInitiatedEvent of(TransferId tid, AccountId src, AccountId tgt,
                                            Money amount, String idempotencyKey) {
        return new TransferInitiatedEvent(UUID.randomUUID(), Instant.now(),
                                          tid, src, tgt, amount, idempotencyKey);
    }
    @Override public String aggregateId() { return transferId.toString(); }
    @Override public String eventType()   { return "TRANSFER_INITIATED"; }
}
