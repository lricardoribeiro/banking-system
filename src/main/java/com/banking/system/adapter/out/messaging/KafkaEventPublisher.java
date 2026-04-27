package com.banking.system.adapter.out.messaging;

import com.banking.system.adapter.out.messaging.event.TransferCompletedKafkaEvent;
import com.banking.system.adapter.out.messaging.event.TransferInitiatedKafkaEvent;
import com.banking.system.application.port.out.PublishDomainEventPort;
import com.banking.system.domain.shared.DomainEvent;
import com.banking.system.domain.transfer.event.TransferCompletedEvent;
import com.banking.system.domain.transfer.event.TransferInitiatedEvent;
import com.banking.system.infrastructure.annotation.MessagingAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Adaptador Kafka que implementa PublishDomainEventPort.
 *
 * Design dos tópicos:
 *   - banking.transfers: eventos do ciclo de vida de transferências (iniciada, concluída, falhou)
 *   - banking.accounts:  eventos de conta (criada, debitada, creditada)
 *   Particionamento por accountId (origem) garante ordenação de eventos por conta.
 *
 * Nota de confiabilidade:
 *   KafkaTemplate.send() é assíncrono. Se o Kafka estiver indisponível, eventos são perdidos.
 *   Para entrega garantida, implemente o Outbox Pattern:
 *     1. Na mesma transação do banco, faça INSERT na tabela 'outbox'.
 *     2. Um conector Debezium (CDC) ou job de polling lê o outbox e publica.
 *     3. Marca a entrada do outbox como publicada.
 *   Isso desacopla o commit do banco da disponibilidade do Kafka.
 *
 * Configuração de produtor idempotente (enable.idempotence=true):
 *   Retentativas do produtor Kafka podem causar duplicatas. Com idempotência habilitada,
 *   o broker deduplica mensagens do mesmo produtor em uma janela de tempo.
 *   Os consumidores DEVEM implementar idempotência mesmo assim (mensagem pode ser entregue
 *   duas vezes se o consumidor cair no meio do processamento).
 */
@MessagingAdapter
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements PublishDomainEventPort {

    static final String TOPICO_TRANSFERENCIAS = "banking.transfers";
    static final String TOPICO_CONTAS         = "banking.accounts";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(DomainEvent evento) {
        switch (evento) {
            case TransferInitiatedEvent e -> publicarTransferenciaIniciada(e);
            case TransferCompletedEvent e -> publicarTransferenciaConcluida(e);
            default -> publicarGenerico(evento);
        }
    }

    private void publicarTransferenciaIniciada(TransferInitiatedEvent e) {
        TransferInitiatedKafkaEvent msg = new TransferInitiatedKafkaEvent(
                e.eventId().toString(), e.transferId().toString(),
                e.sourceAccountId().toString(), e.targetAccountId().toString(),
                e.amount().amount(), e.amount().currency().getCurrencyCode(),
                e.idempotencyKey(), e.occurredAt(), "v1");

        // Chave de partição = sourceAccountId garante ordenação de eventos da mesma conta
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPICO_TRANSFERENCIAS, e.sourceAccountId().toString(), msg);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Falha ao publicar TransferInitiatedEvent {}: {}",
                          e.eventId(), ex.getMessage(), ex);
                // Em produção: gravar no outbox / dead-letter queue
            } else {
                log.debug("TransferInitiatedEvent {} publicado na partição {}",
                          e.eventId(), result.getRecordMetadata().partition());
            }
        });
    }

    private void publicarTransferenciaConcluida(TransferCompletedEvent e) {
        TransferCompletedKafkaEvent msg = new TransferCompletedKafkaEvent(
                e.eventId().toString(), e.transferId().toString(),
                e.sourceAccountId().toString(), e.targetAccountId().toString(),
                e.amount().amount(), e.amount().currency().getCurrencyCode(),
                e.occurredAt(), "v1");

        kafkaTemplate.send(TOPICO_TRANSFERENCIAS, e.sourceAccountId().toString(), msg)
                .whenComplete((r, ex) -> {
                    if (ex != null)
                        log.error("Falha ao publicar TransferCompletedEvent {}", e.eventId(), ex);
                    else
                        log.info("Evento de conclusão da transferência {} publicado", e.transferId());
                });
    }

    private void publicarGenerico(DomainEvent evento) {
        String topico = evento.eventType().startsWith("ACCOUNT") ? TOPICO_CONTAS : TOPICO_TRANSFERENCIAS;
        kafkaTemplate.send(topico, evento.aggregateId(), evento)
                .whenComplete((r, ex) -> {
                    if (ex != null)
                        log.error("Falha ao publicar evento {} tipo={}", evento.eventId(), evento.eventType(), ex);
                });
    }
}
