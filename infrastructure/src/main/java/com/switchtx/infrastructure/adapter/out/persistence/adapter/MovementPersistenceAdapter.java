package com.switchtx.infrastructure.adapter.out.persistence.adapter;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.out.MovementRepositoryPort;
import com.switchtx.domain.model.transaction.Movement;
import com.switchtx.infrastructure.adapter.out.persistence.mapper.PersistenceMapper;
import com.switchtx.infrastructure.adapter.out.persistence.repository.MovementJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MovementPersistenceAdapter implements MovementRepositoryPort {

    private final MovementJpaRepository repository;

    public MovementPersistenceAdapter(MovementJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void saveAll(List<Movement> movements) {
        repository.saveAll(movements.stream().map(PersistenceMapper::toEntity).toList());
    }

    @Override
    public PageResult<Movement> findByAccountId(UUID accountId, PageQuery pageQuery) {
        return PersistenceMapper.toPageResult(
                repository.findByAccountId(accountId, PersistenceMapper.toPageable(pageQuery)),
                PersistenceMapper::toDomain);
    }
}
