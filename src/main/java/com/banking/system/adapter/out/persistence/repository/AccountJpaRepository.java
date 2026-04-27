package com.banking.system.adapter.out.persistence.repository;

import com.banking.system.adapter.out.persistence.entity.AccountJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para contas bancárias.
 *
 * O método findByIdForUpdate adquire lock pessimista (SELECT FOR UPDATE)
 * para garantir exclusão mútua durante transferências concorrentes.
 * A ordem de aquisição dos locks é determinística (lexicográfica por AccountId)
 * para prevenir deadlocks A->B / B->A.
 */
@Repository
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, UUID> {

    /**
     * Busca a conta e adquire lock pessimista de escrita (SELECT FOR UPDATE).
     * Usado pelo TransferMoneyService antes de debitar ou creditar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.id = :id")
    Optional<AccountJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
