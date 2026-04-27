package com.banking.system.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts",
       indexes = { @Index(name = "idx_accounts_owner_id", columnList = "owner_id") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountJpaEntity {

    @Id @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * @Version habilita o locking otimista no JPA.
     * O JPA adiciona "WHERE version = ?" nas instruções UPDATE.
     * Se a linha foi modificada por outra transação, a version mudou
     * e o UPDATE afeta 0 linhas -> JPA lança OptimisticLockException.
     */
    @Version @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
