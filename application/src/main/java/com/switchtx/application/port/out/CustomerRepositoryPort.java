package com.switchtx.application.port.out;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.domain.model.customer.DocumentType;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de clientes. */
public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    PageResult<Customer> findAll(CustomerStatus status, PageQuery pageQuery);

    boolean existsByDocument(DocumentType documentType, String documentNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID excludedId);
}
