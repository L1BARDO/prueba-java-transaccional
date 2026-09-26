package com.switchtx.infrastructure.adapter.in.rest.customer;

import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.domain.model.customer.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Cliente")
public record CustomerResponse(
        UUID id,
        DocumentType documentType,
        String documentNumber,
        String fullName,
        String email,
        String phone,
        CustomerStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getDocumentType(), c.getDocumentNumber(), c.getFullName(),
                c.getEmail(), c.getPhone(), c.getStatus(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
