package com.banking.system.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Entidade JPA para armazenamento de chaves de idempotência.
 * Chaves expiram após 24h e são limpas por job agendado.
 */
@Entity
@Table(name = "idempotency_keys")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyKeyJpaEntity {

    @Id @Column(name = "key_value", length = 64)
    private String keyValue;

    /** Resultado serializado em JSON para retorno em requisições duplicadas. */
    @Column(name = "result_json", length = 4000)
    private String resultJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Marcador de TTL – chaves antigas são limpas por job após 24h. */
    @Column(name = "expires_at")
    private Instant expiresAt;
}
