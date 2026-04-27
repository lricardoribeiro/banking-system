package com.banking.system.adapter.in.messaging;

import com.banking.system.adapter.out.messaging.event.TransferInitiatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka para eventos de transferência.
 *
 * === Consumo Idempotente ===
 *   O Kafka garante entrega AT-LEAST-ONCE (pelo menos uma vez). Um consumidor pode receber
 *   a mesma mensagem múltiplas vezes (ex: após rebalanceamento ou crash antes do commit de offset).
 *   Para tratar isso com segurança:
 *   1. Antes de processar, verifica se o eventId já foi processado.
 *   2. Se sim: pula o processamento (retorna imediatamente).
 *   3. Se não: processa e registra o eventId (no banco ou Redis com TTL).
 *
 * === Acknowledgment Manual (MANUAL_IMMEDIATE) ===
 *   NÃO usamos auto-commit. O offset é confirmado apenas APÓS o processamento bem-sucedido.
 *   Isso garante que, se a aplicação cair durante o processamento, a mensagem seja re-entregue.
 *   Desvantagem: duplicatas em caso de crash -> tratadas por idempotência.
 *
 * === Tratamento de Erros ===
 *   - Erros transitórios (banco indisponível): NÃO confirma offset -> mensagem re-entregue.
 *   - Erros de negócio (dados inválidos): confirma offset para evitar retry infinito -> grava na DLQ.
 *   O DefaultErrorHandler do Spring Kafka com backoff exponencial gerencia as retentativas.
 *
 * === Ordenação ===
 *   O Kafka preserva a ordem dentro de uma partição.
 *   Ao particionar por sourceAccountId, todos os eventos de uma conta chegam em ordem.
 *   NUNCA use consumidores concorrentes na mesma partição (quebraria a ordenação).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferEventConsumer {

    @KafkaListener(
        topics = "banking.transfers",
        groupId = "banking-transfer-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransferEvent(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        log.info("Evento de transferência recebido: topico={} partição={} offset={} chave={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            Object payload = record.value();

            if (payload instanceof TransferInitiatedKafkaEvent evento) {
                processarTransferenciaIniciada(evento);
            } else {
                log.debug("Tipo de evento não tratado: {}", payload.getClass().getSimpleName());
            }

            // Confirma o offset somente após processamento bem-sucedido
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Erro ao processar evento de transferência no offset {}: {}",
                      record.offset(), e.getMessage(), e);
            // NÃO confirma – o tratador de erros do Spring Kafka vai retentar com backoff
            // Após o máximo de retentativas, a mensagem vai para o Dead Letter Topic (banking.transfers.DLT)
            throw e;
        }
    }

    private void processarTransferenciaIniciada(TransferInitiatedKafkaEvent evento) {
        // Exemplo: acionar notificações, atualizar read models (CQRS), etc.
        log.info("Processando TransferInitiatedEvent: transferId={} valor={} {}",
                evento.transferId(), evento.amount(), evento.currency());
        // Em uma arquitetura CQRS completa, isto atualizaria uma projeção de read model.
    }
}
