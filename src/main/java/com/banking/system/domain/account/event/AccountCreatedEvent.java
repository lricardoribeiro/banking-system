package com.banking.system.domain.account.event;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/** Evento de domínio disparado quando uma nova conta bancária é aberta. */
public record AccountCreatedEvent(
        UUID eventId, Instant occurredAt,
        AccountId accountId, String ownerId, Currency currency
) implements DomainEvent {
    public static AccountCreatedEvent of(AccountId id, String owner, Currency ccy) {
        return new AccountCreatedEvent(UUID.randomUUID(), Instant.now(), id, owner, ccy);
    }
    @Override public String aggregateId() { return accountId.toString(); }
    @Override public String eventType()   { return "ACCOUNT_CREATED"; }
}
