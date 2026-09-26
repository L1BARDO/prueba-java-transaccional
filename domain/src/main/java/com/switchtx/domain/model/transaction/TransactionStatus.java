package com.switchtx.domain.model.transaction;

public enum TransactionStatus {
    COMPLETED,  // Aplicada: los saldos fueron afectados y existen movimientos contables
    REJECTED    // Rechazada por una regla de negocio: no afecta saldos, queda para auditoría
}
