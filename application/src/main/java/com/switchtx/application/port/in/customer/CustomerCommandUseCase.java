package com.switchtx.application.port.in.customer;

import com.switchtx.domain.model.customer.Customer;

import java.util.UUID;

/** Puerto de entrada: operaciones que modifican clientes. */
public interface CustomerCommandUseCase {

    Customer register(RegisterCustomerCommand command);

    Customer update(UpdateCustomerCommand command);

    /** Baja lógica: el cliente pasa a INACTIVE. Requiere que no tenga cuentas abiertas. */
    void deactivate(UUID customerId);
}
