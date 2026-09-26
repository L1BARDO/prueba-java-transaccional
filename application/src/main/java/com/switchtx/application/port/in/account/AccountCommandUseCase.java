package com.switchtx.application.port.in.account;

import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountStatus;

import java.util.UUID;

/** Puerto de entrada: operaciones que modifican cuentas. */
public interface AccountCommandUseCase {

    Account open(OpenAccountCommand command);

    /** Bloquea o reactiva una cuenta (ACTIVE ⇄ BLOCKED). */
    Account changeStatus(UUID accountId, AccountStatus targetStatus);

    /** Cierre definitivo de la cuenta. Requiere saldo cero. */
    void close(UUID accountId);
}
