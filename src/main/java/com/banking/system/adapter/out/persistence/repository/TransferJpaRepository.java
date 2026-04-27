package com.banking.system.adapter.out.persistence.repository;

import com.banking.system.adapter.out.persistence.entity.TransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para transferências.
 *
 * O campo idempotency_key possui constraint UNIQUE no banco,
 * garantindo idempotência em nível de banco de dados.
 * Inserções duplicadas lançam DataIntegrityViolationException,
 * que é capturada pelo TransferMoneyService.
 */
@Repository
public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, UUID> {

    Optional<TransferJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
