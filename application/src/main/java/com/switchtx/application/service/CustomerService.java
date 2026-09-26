package com.switchtx.application.service;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.in.customer.CustomerCommandUseCase;
import com.switchtx.application.port.in.customer.CustomerQueryUseCase;
import com.switchtx.application.port.in.customer.RegisterCustomerCommand;
import com.switchtx.application.port.in.customer.UpdateCustomerCommand;
import com.switchtx.application.port.out.AccountRepositoryPort;
import com.switchtx.application.port.out.CustomerRepositoryPort;
import com.switchtx.application.port.out.UnitOfWork;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class CustomerService implements CustomerCommandUseCase, CustomerQueryUseCase {

    private static final Logger log = LogManager.getLogger(CustomerService.class);

    private final CustomerRepositoryPort customers;
    private final AccountRepositoryPort accounts;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CustomerService(CustomerRepositoryPort customers, AccountRepositoryPort accounts,
                           UnitOfWork unitOfWork, Clock clock) {
        this.customers = Objects.requireNonNull(customers);
        this.accounts = Objects.requireNonNull(accounts);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Customer register(RegisterCustomerCommand command) {
        Customer customer = Customer.register(command.documentType(), command.documentNumber(), command.fullName(),
                command.email(), command.phone(), clock.instant());

        Customer saved = unitOfWork.execute(() -> {
            if (customers.existsByDocument(customer.getDocumentType(), customer.getDocumentNumber())) {
                throw new ConflictException(ErrorCode.CUSTOMER_ALREADY_EXISTS,
                        "Ya existe un cliente con documento %s %s"
                                .formatted(customer.getDocumentType(), customer.getDocumentNumber()));
            }
            if (customers.existsByEmail(customer.getEmail())) {
                throw emailInUse(customer.getEmail());
            }
            return customers.save(customer);
        });
        log.info("Cliente registrado id={} documento={}", saved.getId(), saved.getDocumentType());
        return saved;
    }

    @Override
    public Customer update(UpdateCustomerCommand command) {
        Customer updated = unitOfWork.execute(() -> {
            Customer customer = findOrThrow(command.customerId());
            String email = command.email() == null ? null : command.email().trim().toLowerCase(Locale.ROOT);
            if (email != null && customers.existsByEmailAndIdNot(email, customer.getId())) {
                throw emailInUse(email);
            }
            customer.updateContactInfo(command.fullName(), command.email(), command.phone(), clock.instant());
            return customers.save(customer);
        });
        log.info("Cliente actualizado id={}", updated.getId());
        return updated;
    }

    @Override
    public void deactivate(UUID customerId) {
        unitOfWork.run(() -> {
            Customer customer = findOrThrow(customerId);
            if (accounts.existsOpenAccountsForCustomer(customerId)) {
                throw new BusinessRuleViolationException(ErrorCode.CUSTOMER_HAS_OPEN_ACCOUNTS,
                        "El cliente %s tiene cuentas abiertas; ciérrelas antes de inactivarlo".formatted(customerId));
            }
            customer.deactivate(clock.instant());
            customers.save(customer);
        });
        log.info("Cliente inactivado id={}", customerId);
    }

    @Override
    public Customer getById(UUID customerId) {
        return unitOfWork.executeReadOnly(() -> findOrThrow(customerId));
    }

    @Override
    public PageResult<Customer> search(CustomerStatus status, PageQuery pageQuery) {
        return unitOfWork.executeReadOnly(() -> customers.findAll(status, pageQuery));
    }

    private Customer findOrThrow(UUID customerId) {
        return customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                        "No existe el cliente %s".formatted(customerId)));
    }

    private static ConflictException emailInUse(String email) {
        return new ConflictException(ErrorCode.CUSTOMER_ALREADY_EXISTS,
                "El correo %s ya está registrado".formatted(email));
    }
}
