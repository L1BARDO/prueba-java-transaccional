package com.switchtx.infrastructure.adapter.out.persistence.repository;

import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.infrastructure.adapter.out.persistence.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID>,
        JpaSpecificationExecutor<AccountEntity> {

    /** SELECT ... FOR UPDATE */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.id = :id")
    Optional<AccountEntity> findByIdForUpdate(@Param("id") UUID id);

    /** SELECT ... ORDER BY id FOR UPDATE: bloqueo en orden determinista para evitar deadlocks. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.id in :ids order by a.id")
    List<AccountEntity> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

    boolean existsByCustomerIdAndStatusNot(UUID customerId, AccountStatus status);

    @Query(value = "select nextval('account_number_seq')", nativeQuery = true)
    long nextAccountNumber();
}
