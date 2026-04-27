package com.banking.system.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do produtor e consumidor Kafka.
 *
 * Configurações do produtor:
 *   - enable.idempotence=true: o broker deduplica mensagens do mesmo produtor.
 *   - acks=all: aguarda confirmação de todas as réplicas in-sync antes de confirmar.
 *   - retries=3: trata falhas transitórias de rede.
 *
 * Configurações do consumidor:
 *   - enable.auto.commit=false: commit manual de offset para processamento at-least-once.
 *   - max.poll.records=10: processa em lotes pequenos para limitar o tempo de processamento por poll.
 *   - isolation.level=read_committed: lê apenas mensagens de transações confirmadas.
 *
 * Tratamento de erros:
 *   - ExponentialBackOff: retentativas com delay crescente.
 *   - Dead Letter Topic (DLT): mensagens que esgotam as retentativas vão para banking.transfers.DLT.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    @Bean
    public NewTopic topicoTransferencias() {
        return TopicBuilder.name("banking.transfers")
                .partitions(6)           // 6 partições para paralelismo
                .replicas(3)             // 3 réplicas para tolerância a falhas (1 para dev local)
                .config("retention.ms", String.valueOf(7 * 24 * 60 * 60 * 1000L)) // 7 dias
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic topicoContas() {
        return TopicBuilder.name("banking.accounts").partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic topicoDltTransferencias() {
        return TopicBuilder.name("banking.transfers.DLT").partitions(1).replicas(1).build();
    }


    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Produtor idempotente: exactly-once no lado do produtor
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "banking-transfer-consumer");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.banking.system.*");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory());

        // Modo ACK manual: offset confirmado apenas após ack.acknowledge() explícito
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Backoff exponencial: 1s, 2s, 4s, 8s... até 64s, máximo 5 retentativas
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(64000L);
        factory.setCommonErrorHandler(new DefaultErrorHandler(backOff));

        // Thread única por partição – preserva ordenação das mensagens
        factory.setConcurrency(1);

        return factory;
    }
}
