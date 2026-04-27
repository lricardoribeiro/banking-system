package com.banking.system.domain.transfer.event;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.shared.DomainEvent;
import com.banking.system.domain.transfer.TransferId;
import java.time.Instant;
import java.util.UUID;

/** Evento de domínio disparado quando uma transferência é concluída com sucesso. */
public record TransferCompletedEvent(
        UUID eventId, Instant occurredAt,
        TransferId transferId, AccountId sourceAccountId,
        AccountId targetAccountId, Money amount
) implements DomainEvent {
    public static TransferCompletedEvent of(TransferId tid, AccountId src, AccountId tgt, Money amount) {
        return new TransferCompletedEvent(UUID.randomUUID(), Instant.now(), tid, src, tgt, amount);
    }
    @Override public String aggregateId() { return transferId.toString(); }
    @Override public String eventType()   { return "TRANSFER_COMPLETED"; }
}
