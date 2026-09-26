package com.switchtx.infrastructure.adapter.in.rest.customer;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.port.in.customer.CustomerCommandUseCase;
import com.switchtx.application.port.in.customer.CustomerQueryUseCase;
import com.switchtx.application.port.in.customer.RegisterCustomerCommand;
import com.switchtx.application.port.in.customer.UpdateCustomerCommand;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.infrastructure.adapter.in.rest.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private final CustomerCommandUseCase commands;
    private final CustomerQueryUseCase queries;

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public ResponseEntity<CustomerResponse> create(CreateCustomerRequest request) {
        Customer customer = commands.register(new RegisterCustomerCommand(request.documentType(),
                request.documentNumber(), request.fullName(), request.email(), request.phone()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(customer.getId()).toUri();
        return ResponseEntity.created(location).body(CustomerResponse.from(customer));
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public CustomerResponse getById(UUID customerId) {
        return CustomerResponse.from(queries.getById(customerId));
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public PageResponse<CustomerResponse> search(CustomerStatus status, int page, int size) {
        return PageResponse.from(queries.search(status, new PageQuery(page, size)), CustomerResponse::from);
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    public CustomerResponse update(UUID customerId, UpdateCustomerRequest request) {
        return CustomerResponse.from(commands.update(new UpdateCustomerCommand(customerId, request.fullName(),
                request.email(), request.phone())));
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_DELETE')")
    public ResponseEntity<Void> deactivate(UUID customerId) {
        commands.deactivate(customerId);
        return ResponseEntity.noContent().build();
    }
}
