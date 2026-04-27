package com.banking.system.application.port.out;

import java.util.Optional;

/**
 * Porta Secundária: gerenciamento de chaves de idempotência.
 *
 * Mecanismo:
 *   1. Antes de executar uma operação, verifica se a chave já existe.
 *   2. Se existir: retorna o resultado armazenado (pula a re-execução).
 *   3. Se não existir: executa a operação e armazena o resultado atomicamente.
 *
 * A verificação-e-armazenamento DEVE ser atômica (constraint UNIQUE no banco + INSERT).
 * Um SELECT seguido de INSERT (duas operações) tem race condition TOCTOU:
 *   dois requests concorrentes com a mesma chave podem ambos passar pelo SELECT
 *   e ambos prosseguir para o INSERT, causando execução duplicada.
 * A constraint UNIQUE na coluna idempotency_key garante que apenas um INSERT vença;
 * o perdedor recebe violação de constraint, tratada como "já processado".
 */
public interface IdempotencyPort {

    /** Retorna true se esta chave já foi processada. */
    boolean exists(String idempotencyKey);

    /** Armazena a chave de idempotência atomicamente. Lança exceção se a chave já existe. */
    void store(String idempotencyKey, String resultJson);

    /** Retorna o JSON do resultado armazenado para uma chave já processada. */
    Optional<String> getResult(String idempotencyKey);
}
