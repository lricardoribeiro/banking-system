package com.banking.system.application.port.out;

import com.banking.system.domain.shared.DomainEvent;

/**
 * Porta Secundária: publicação de eventos de domínio no barramento de eventos (Kafka).
 *
 * Em produção, combine com o Outbox Pattern para garantia de entrega:
 *   1. Dentro da transação do banco, insira os eventos na tabela 'outbox'.
 *   2. Um relay separado (Debezium CDC ou job de polling) lê o outbox
 *      e publica no Kafka, depois marca as entradas como publicadas.
 *   Isso garante entrega pelo menos uma vez mesmo se a aplicação cair
 *   entre o commit do banco e a publicação no Kafka.
 *
 *   A implementação atual publica diretamente (mais simples, mas arrisca
 *   perda de evento se o Kafka estiver indisponível no momento do commit).
 */
public interface PublishDomainEventPort {
    void publish(DomainEvent event);
}
