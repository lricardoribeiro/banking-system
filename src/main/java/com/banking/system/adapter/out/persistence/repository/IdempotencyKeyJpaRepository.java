package com.banking.system.adapter.out.persistence.repository;

import com.banking.system.adapter.out.persistence.entity.IdempotencyKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;

/**
 * Repositório JPA para chaves de idempotência.
 *
 * Chaves expiradas devem ser removidas periodicamente via job agendado
 * para evitar crescimento ilimitado da tabela.
 * Sugestão: executar deleteByExpiresAtBefore(Instant.now()) diariamente.
 */
@Repository
public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, String> {

    void deleteByExpiresAtBefore(Instant cutoff);
}
