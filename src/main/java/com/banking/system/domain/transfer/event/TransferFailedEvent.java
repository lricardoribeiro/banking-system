package com.banking.system.domain.transfer.event;

import com.banking.system.domain.shared.DomainEvent;
import com.banking.system.domain.transfer.TransferId;
import java.time.Instant;
import java.util.UUID;

/** Evento de domínio disparado quando uma transferência falha. */
public record TransferFailedEvent(
        UUID eventId, Instant occurredAt,
        TransferId transferId, String reason
) implements DomainEvent {
    public static TransferFailedEvent of(TransferId tid, String reason) {
        return new TransferFailedEvent(UUID.randomUUID(), Instant.now(), tid, reason);
    }
    @Override public String aggregateId() { return transferId.toString(); }
    @Override public String eventType()   { return "TRANSFER_FAILED"; }
}
