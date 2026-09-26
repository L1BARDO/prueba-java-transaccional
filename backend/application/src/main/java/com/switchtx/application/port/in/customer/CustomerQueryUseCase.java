package com.switchtx.application.port.in.customer;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;

import java.util.UUID;

/** Puerto de entrada: consultas de clientes. */
public interface CustomerQueryUseCase {

    Customer getById(UUID customerId);

    PageResult<Customer> search(CustomerStatus status, PageQuery pageQuery);
}
