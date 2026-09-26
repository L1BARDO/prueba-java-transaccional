package com.switchtx.infrastructure.adapter.out.persistence.adapter;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.out.CustomerRepositoryPort;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.domain.model.customer.DocumentType;
import com.switchtx.infrastructure.adapter.out.persistence.mapper.ConstraintViolations;
import com.switchtx.infrastructure.adapter.out.persistence.mapper.PersistenceMapper;
import com.switchtx.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;
import com.switchtx.infrastructure.adapter.out.persistence.repository.Specifications;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository repository;

    @Override
    public Customer save(Customer customer) {
        try {
            return PersistenceMapper.toDomain(repository.saveAndFlush(PersistenceMapper.toEntity(customer)));
        } catch (DataIntegrityViolationException ex) {
            // Carrera entre la validación previa y el INSERT: la restricción única de BD es la última defensa
            if (ConstraintViolations.isViolationOf(ex, "uk_customers_document")
                    || ConstraintViolations.isViolationOf(ex, "uk_customers_email")) {
                throw new ConflictException(ErrorCode.CUSTOMER_ALREADY_EXISTS,
                        "Ya existe un cliente con el mismo documento o correo", ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Customer> findAll(CustomerStatus status, PageQuery pageQuery) {
        return PersistenceMapper.toPageResult(
                repository.findAll(Specifications.customers(status), PersistenceMapper.toPageable(pageQuery)),
                PersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByDocument(DocumentType documentType, String documentNumber) {
        return repository.existsByDocumentTypeAndDocumentNumber(documentType, documentNumber);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID excludedId) {
        return repository.existsByEmailAndIdNot(email, excludedId);
    }
}
