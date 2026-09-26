package com.switchtx.domain.model.transaction;

public enum TransactionType {
    DEPOSIT,     // Consignación: solo cuenta destino
    WITHDRAWAL,  // Retiro: solo cuenta origen
    TRANSFER     // Transferencia: cuenta origen y destino
}
