package com.banking.system.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries",
       indexes = {
           @Index(name = "idx_ledger_account_id", columnList = "account_id"),
           @Index(name = "idx_ledger_transfer_id", columnList = "transfer_id"),
           @Index(name = "idx_ledger_account_created", columnList = "account_id, created_at")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntryJpaEntity {

    @Id @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "transfer_id", nullable = false, updatable = false)
    private UUID transferId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    /**
     * DEBIT ou CREDIT. Armazenado como string para legibilidade em queries.
     * Uma constraint CHECK no banco garante valores válidos.
     */
    @Column(name = "entry_type", nullable = false, length = 10, updatable = false)
    private String entryType;

    /**
     * Sempre positivo. A direção é expressa pelo entryType.
     * DECIMAL(19,4) – 4 casas decimais para precisão interna, arredondado
     * para 2 na exibição. Suporta valores até 999.999.999.999.999,9999
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "description", length = 255, updatable = false)
    private String description;

    /** Imutável – entradas nunca são atualizadas, apenas inseridas. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
