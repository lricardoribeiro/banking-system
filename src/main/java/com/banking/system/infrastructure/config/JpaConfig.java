package com.banking.system.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuração do JPA e gerenciamento de transações.
 *
 * Estratégia de isolamento de transação:
 * - Padrão (READ_COMMITTED): usado para consultas somente-leitura.
 * - REPEATABLE_READ: usado no TransferMoneyService para evitar phantom reads
 *   de novos lançamentos inseridos no ledger entre a verificação de saldo e a escrita.
 *
 * @Transactional explícito nos serviços de caso de uso com o nível correto de isolamento
 * é preferível à configuração global para documentar a intenção por caso de uso.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.banking.system.adapter.out.persistence.repository")
public class JpaConfig {
    // Gerenciador de transações customizado pode ser adicionado aqui se necessário (ex: XA/2PC)
}
