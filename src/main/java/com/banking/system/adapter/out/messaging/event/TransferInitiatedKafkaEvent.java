package com.banking.system.adapter.out.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Schema da mensagem Kafka para TransferInitiatedEvent.
 * Record plano para desacoplar o contrato de mensagens do modelo de domínio.
 *
 * Versionamento: adicione novos campos com valores padrão; nunca remova ou renomeie campos.
 */
public record TransferInitiatedKafkaEvent(
        String eventId,
        String transferId,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        Instant occurredAt,
        String eventVersion     // "v1" – para futura evolução de schema
) {}
