package com.banking.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Ponto de entrada da aplicação Banking System.
 *
 * Arquitetura: Hexagonal (Ports & Adapters) com DDD.
 * Paradigma: Consistência primeiro (CP do CAP Theorem) para operações financeiras.
 *
 * Camadas:
 *   - domain:          entidades ricas, agregados, value objects, eventos de domínio
 *   - application:     casos de uso, ports (interfaces)
 *   - adapter/in:      REST controllers, consumers Kafka
 *   - adapter/out:     repositórios JPA, publishers Kafka
 *   - infrastructure:  configurações Spring, segurança, observabilidade
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
public class BankingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingSystemApplication.class, args);
    }
}
