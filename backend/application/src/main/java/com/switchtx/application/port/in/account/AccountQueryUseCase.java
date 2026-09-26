package com.switchtx.application.port.in.account;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.transaction.Movement;

import java.util.UUID;

/** Puerto de entrada: consultas de cuentas y su extracto. */
public interface AccountQueryUseCase {

    Account getById(UUID accountId);

    PageResult<Account> search(AccountFilter filter, PageQuery pageQuery);

    /** Extracto de la cuenta: movimientos contables del más reciente al más antiguo. */
    PageResult<Movement> getMovements(UUID accountId, PageQuery pageQuery);
}
