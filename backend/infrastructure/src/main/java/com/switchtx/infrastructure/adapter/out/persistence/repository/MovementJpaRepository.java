package com.switchtx.infrastructure.adapter.out.persistence.repository;

import com.switchtx.infrastructure.adapter.out.persistence.entity.MovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MovementJpaRepository extends JpaRepository<MovementEntity, UUID> {

    Page<MovementEntity> findByAccountId(UUID accountId, Pageable pageable);
}
