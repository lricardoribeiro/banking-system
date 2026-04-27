package com.banking.system.adapter.out.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Schema da mensagem Kafka para TransferCompletedEvent.
 * Mantida como um record plano (sem tipos de domínio aninhados) para manter o
 * contrato de mensagens estável e independente de mudanças no modelo de domínio.
 *
 * Versionamento: adicione novos campos com valores padrão; nunca remova ou renomeie campos.
 * Em produção, use Avro ou Protobuf com um schema registry para evolução controlada de schema.
 */
public record TransferCompletedKafkaEvent(
        String eventId,
        String transferId,
        String sourceAccountId,
        String targetAccountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        String eventVersion     // "v1" – para futura evolução de schema
) {}
