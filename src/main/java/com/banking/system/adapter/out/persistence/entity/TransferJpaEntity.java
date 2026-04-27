package com.banking.system.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers",
       indexes = {
           @Index(name = "idx_transfers_idempotency_key", columnList = "idempotency_key", unique = true),
           @Index(name = "idx_transfers_source_account", columnList = "source_account_id"),
           @Index(name = "idx_transfers_status", columnList = "status")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferJpaEntity {

    @Id @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "source_account_id", nullable = false, updatable = false)
    private UUID sourceAccountId;

    @Column(name = "target_account_id", nullable = false, updatable = false)
    private UUID targetAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    /**
     * Constraint UNIQUE garante idempotência no nível do banco.
     * Mesmo que dois requests concorrentes passem pelo check da aplicação,
     * apenas um INSERT terá sucesso; o outro receberá violação de constraint.
     */
    @Column(name = "idempotency_key", nullable = false, length = 64, updatable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
