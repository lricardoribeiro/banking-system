package com.banking.system.domain.account.event;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.shared.DomainEvent;
import com.banking.system.domain.transfer.TransferId;
import java.time.Instant;
import java.util.UUID;

/** Evento de domínio disparado quando um valor é debitado de uma conta. */
public record MoneyDebitedEvent(
        UUID eventId, Instant occurredAt,
        AccountId accountId, Money amount, TransferId transferId
) implements DomainEvent {
    public static MoneyDebitedEvent of(AccountId id, Money amount, TransferId tid, Instant at) {
        return new MoneyDebitedEvent(UUID.randomUUID(), at, id, amount, tid);
    }
    @Override public String aggregateId() { return accountId.toString(); }
    @Override public String eventType()   { return "MONEY_DEBITED"; }
}
