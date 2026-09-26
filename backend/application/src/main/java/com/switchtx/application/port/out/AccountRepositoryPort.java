package com.switchtx.application.port.out;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.in.account.AccountFilter;
import com.switchtx.domain.model.account.Account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de cuentas. */
public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findById(UUID id);

    /** Obtiene la cuenta con bloqueo exclusivo (pesimista) hasta el fin de la transacción. */
    Optional<Account> findByIdForUpdate(UUID id);

    /**
     * Bloquea varias cuentas siempre en el mismo orden (por id) para evitar interbloqueos
     * entre transferencias cruzadas concurrentes.
     */
    List<Account> findAllByIdForUpdate(Collection<UUID> ids);

    PageResult<Account> findAll(AccountFilter filter, PageQuery pageQuery);

    boolean existsOpenAccountsForCustomer(UUID customerId);

    /** Genera el siguiente número de cuenta único. */
    String nextAccountNumber();
}
