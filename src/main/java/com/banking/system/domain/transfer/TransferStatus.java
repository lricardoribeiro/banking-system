package com.banking.system.domain.transfer;

/**
 * Máquina de estados de uma Transferência:
 *
 *   PENDENTE ──► CONCLUÍDA        (caminho feliz: débito+crédito realizados)
 *   PENDENTE ──► FALHOU           (saldo insuficiente, conta bloqueada, etc.)
 *   PENDENTE ──► COMPENSANDO      (rollback de saga em cenário distribuído)
 *   COMPENSANDO ──► COMPENSADA    (compensação concluída)
 */
public enum TransferStatus {
    PENDING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
