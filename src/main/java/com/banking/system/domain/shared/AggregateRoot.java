package com.banking.system.domain.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classe base para todos os Aggregate Roots.
 *
 * Um Aggregate Root é a fronteira de consistência de um conjunto de objetos de domínio.
 * Todas as modificações em entidades dentro de um aggregate devem passar pela raiz.
 *
 * Padrão de Eventos de Domínio:
 *   Os eventos são acumulados em memória durante o ciclo de vida do aggregate.
 *   O serviço de aplicação os despacha APÓS o commit da transação.
 *   Isso é crítico: se publicássemos eventos antes do commit e o banco revertesse,
 *   os consumidores agiriam sobre dados que não existem.
 *
 *   Para entrega garantida mesmo em caso de crash da aplicação, combine com o Outbox Pattern:
 *   persista os eventos na tabela 'outbox' dentro da mesma transação, então
 *   um processo de retransmissão separado os publica no Kafka.
 *
 * @param <ID> o tipo fortemente tipado da identidade do aggregate
 */
public abstract class AggregateRoot<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** Acumula um evento de domínio para ser despachado após o commit. */
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /** Retorna uma visão não modificável dos eventos de domínio pendentes. */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /** Chamado pelo serviço de aplicação após os eventos serem publicados. */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public abstract ID getId();
}
