package com.switchtx.domain.model.account;

/**
 * Ciclo de vida de una cuenta.
 * ACTIVE ⇄ BLOCKED; ACTIVE/BLOCKED → CLOSED (estado final, requiere saldo cero).
 */
public enum AccountStatus {
    ACTIVE,
    BLOCKED,
    CLOSED
}
