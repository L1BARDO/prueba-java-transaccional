package com.switchtx.infrastructure.adapter.out.persistence.adapter;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.in.account.AccountFilter;
import com.switchtx.application.port.out.AccountRepositoryPort;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.infrastructure.adapter.out.persistence.mapper.PersistenceMapper;
import com.switchtx.infrastructure.adapter.out.persistence.repository.AccountJpaRepository;
import com.switchtx.infrastructure.adapter.out.persistence.repository.Specifications;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository repository;

    public AccountPersistenceAdapter(AccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Account save(Account account) {
        return PersistenceMapper.toDomain(repository.save(PersistenceMapper.toEntity(account)));
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<Account> findAllByIdForUpdate(Collection<UUID> ids) {
        return repository.findAllByIdForUpdate(ids).stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public PageResult<Account> findAll(AccountFilter filter, PageQuery pageQuery) {
        return PersistenceMapper.toPageResult(
                repository.findAll(Specifications.accounts(filter), PersistenceMapper.toPageable(pageQuery)),
                PersistenceMapper::toDomain);
    }

    @Override
    public boolean existsOpenAccountsForCustomer(UUID customerId) {
        return repository.existsByCustomerIdAndStatusNot(customerId, AccountStatus.CLOSED);
    }

    @Override
    public String nextAccountNumber() {
        return String.valueOf(repository.nextAccountNumber());
    }
}
