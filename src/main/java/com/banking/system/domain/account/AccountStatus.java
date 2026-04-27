package com.banking.system.domain.account;

/**
 * Estados do ciclo de vida de uma conta bancária.
 *
 * Transições válidas:
 *   ATIVO   ──► BLOQUEADO   (detecção de fraude, intervenção manual)
 *   BLOQUEADO ──► ATIVO     (desbloqueio após análise)
 *   ATIVO   ──► ENCERRADO   (estado terminal – solicitação do cliente ou inatividade)
 *   BLOQUEADO ──► ENCERRADO (estado terminal)
 */
public enum AccountStatus { ACTIVE, BLOCKED, CLOSED }
