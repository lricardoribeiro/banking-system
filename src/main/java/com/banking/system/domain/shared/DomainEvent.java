package com.banking.system.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Interface base para todos os Eventos de Domínio.
 *
 * Eventos de Domínio representam fatos imutáveis que ocorreram no domínio.
 * São o mecanismo pelo qual Aggregates se comunicam entre contextos delimitados
 * sem acoplamento direto (via barramento de eventos / Kafka).
 *
 * Decisões de design:
 * - Interface (não classe abstrata) mantém o domínio livre de acoplamento de herança.
 * - Records são implementadores ideais: imutáveis + sintaxe compacta.
 * - eventId garante unicidade global para processamento idempotente.
 * - aggregateId habilita roteamento e ordenação de eventos por entidade.
 */
public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
    String aggregateId();
    String eventType();
}
