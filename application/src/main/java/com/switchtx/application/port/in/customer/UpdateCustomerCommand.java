package com.switchtx.application.port.in.customer;

import java.util.UUID;

public record UpdateCustomerCommand(UUID customerId, String fullName, String email, String phone) {
}
