package com.switchtx.application.port.in.customer;

import com.switchtx.domain.model.customer.DocumentType;

public record RegisterCustomerCommand(DocumentType documentType,
                                      String documentNumber,
                                      String fullName,
                                      String email,
                                      String phone) {
}
