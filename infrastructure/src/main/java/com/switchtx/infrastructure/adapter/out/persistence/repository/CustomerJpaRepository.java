package com.switchtx.infrastructure.adapter.out.persistence.repository;

import com.switchtx.domain.model.customer.DocumentType;
import com.switchtx.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID>,
        JpaSpecificationExecutor<CustomerEntity> {

    boolean existsByDocumentTypeAndDocumentNumber(DocumentType documentType, String documentNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
