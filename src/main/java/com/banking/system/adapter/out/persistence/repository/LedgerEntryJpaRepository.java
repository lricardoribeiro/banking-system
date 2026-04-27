package com.banking.system.adapter.out.persistence.repository;

import com.banking.system.adapter.out.persistence.entity.LedgerEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para lançamentos do livro-razão.
 *
 * A tabela ledger_entries é de apenas-inserção (append-only):
 * nunca deve ser atualizada ou deletada em produção.
 * A query de saldo calcula: SUM(CRÉDITOS) - SUM(DÉBITOS).
 */
@Repository
public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

    List<LedgerEntryJpaEntity> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    List<LedgerEntryJpaEntity> findByTransferId(UUID transferId);

    /**
     * Calcula o saldo atual de uma conta diretamente do livro-razão.
     * Saldo = SUM(CRÉDITO) - SUM(DÉBITO).
     * Retorna 0 para contas sem lançamentos (via COALESCE).
     * Executada com isolamento REPEATABLE_READ no TransferMoneyService para
     * evitar phantom reads durante a transação de transferência.
     */
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount " +
                   "WHEN e.entry_type = 'DEBIT' THEN -e.amount ELSE 0 END), 0) " +
                   "FROM ledger_entries e WHERE e.account_id = :accountId",
           nativeQuery = true)
    BigDecimal calculateBalance(UUID accountId);
}
