package com.switchtx.application.port.in.account;

import com.switchtx.domain.model.account.AccountStatus;

import java.util.UUID;

/** Criterios opcionales de búsqueda de cuentas (null = sin filtro). */
public record AccountFilter(UUID customerId, AccountStatus status) {
}
