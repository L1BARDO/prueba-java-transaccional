package com.switchtx.application.port.out;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.domain.model.transaction.Movement;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: persistencia del libro de movimientos (ledger). */
public interface MovementRepositoryPort {

    void saveAll(List<Movement> movements);

    PageResult<Movement> findByAccountId(UUID accountId, PageQuery pageQuery);
}
