package com.switchtx.application.service;

import com.switchtx.application.port.in.customer.RegisterCustomerCommand;
import com.switchtx.application.port.in.customer.UpdateCustomerCommand;
import com.switchtx.application.port.out.AccountRepositoryPort;
import com.switchtx.application.port.out.CustomerRepositoryPort;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.domain.model.customer.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepository;

    @Mock
    private AccountRepositoryPort accountRepository;

    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerRepository, accountRepository, new FakeUnitOfWork(), clock);
    }

    @Test
    @DisplayName("Registra un cliente nuevo cuando el documento y correo no existen")
    void shouldRegisterNewCustomer() {
        RegisterCustomerCommand command = new RegisterCustomerCommand(DocumentType.CC, "1234567890",
                "Juan Pérez", "juan.perez@example.com", "+573001234567");

        given(customerRepository.existsByDocument(DocumentType.CC, "1234567890")).willReturn(false);
        given(customerRepository.existsByEmail("juan.perez@example.com")).willReturn(false);
        given(customerRepository.save(any(Customer.class))).willAnswer(inv -> inv.getArgument(0));

        Customer created = service.register(command);

        assertThat(created).isNotNull();
        assertThat(created.getDocumentType()).isEqualTo(DocumentType.CC);
        assertThat(created.getDocumentNumber()).isEqualTo("1234567890");
        assertThat(created.getFullName()).isEqualTo("Juan Pérez");
        assertThat(created.getEmail()).isEqualTo("juan.perez@example.com");
        assertThat(created.getStatus()).isEqualTo(CustomerStatus.ACTIVE);

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Rechaza registrar cliente si el documento ya está en uso")
    void shouldRejectRegisterWhenDocumentAlreadyExists() {
        RegisterCustomerCommand command = new RegisterCustomerCommand(DocumentType.CC, "1234567890",
                "Juan Pérez", "juan.perez@example.com", null);

        given(customerRepository.existsByDocument(DocumentType.CC, "1234567890")).willReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_ALREADY_EXISTS);

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza registrar cliente si el correo ya está en uso")
    void shouldRejectRegisterWhenEmailAlreadyExists() {
        RegisterCustomerCommand command = new RegisterCustomerCommand(DocumentType.CC, "1234567890",
                "Juan Pérez", "juan.perez@example.com", null);

        given(customerRepository.existsByDocument(DocumentType.CC, "1234567890")).willReturn(false);
        given(customerRepository.existsByEmail("juan.perez@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_ALREADY_EXISTS);

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualiza datos de contacto del cliente exitosamente")
    void shouldUpdateCustomerContactInfo() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.restore(customerId, DocumentType.CC, "1234567890", "Juan Pérez",
                "juan.perez@example.com", null, CustomerStatus.ACTIVE, now, now, 1L);

        given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
        given(customerRepository.existsByEmailAndIdNot("nuevo.email@example.com", customerId)).willReturn(false);
        given(customerRepository.save(any(Customer.class))).willAnswer(inv -> inv.getArgument(0));

        UpdateCustomerCommand command = new UpdateCustomerCommand(customerId, "Juan P. Gómez",
                "nuevo.email@example.com", "+573119876543");

        Customer updated = service.update(command);

        assertThat(updated.getFullName()).isEqualTo("Juan P. Gómez");
        assertThat(updated.getEmail()).isEqualTo("nuevo.email@example.com");
        assertThat(updated.getPhone()).isEqualTo("+573119876543");
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("Rechaza actualización si el nuevo correo pertenece a otro cliente")
    void shouldRejectUpdateWhenEmailBelongsToAnotherCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.restore(customerId, DocumentType.CC, "1234567890", "Juan Pérez",
                "juan.perez@example.com", null, CustomerStatus.ACTIVE, now, now, 1L);

        given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
        given(customerRepository.existsByEmailAndIdNot("otro@example.com", customerId)).willReturn(true);

        UpdateCustomerCommand command = new UpdateCustomerCommand(customerId, "Juan",
                "otro@example.com", null);

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_ALREADY_EXISTS);

        verify(customerRepository, never()).save(customer);
    }

    @Test
    @DisplayName("Desactiva cliente sin cuentas abiertas")
    void shouldDeactivateCustomerWithoutOpenAccounts() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.restore(customerId, DocumentType.CC, "1234567890", "Juan Pérez",
                "juan.perez@example.com", null, CustomerStatus.ACTIVE, now, now, 1L);

        given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
        given(accountRepository.existsOpenAccountsForCustomer(customerId)).willReturn(false);

        service.deactivate(customerId);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CustomerStatus.INACTIVE);
    }

    @Test
    @DisplayName("Rechaza desactivar cliente con cuentas abiertas")
    void shouldRejectDeactivateCustomerWithOpenAccounts() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.restore(customerId, DocumentType.CC, "1234567890", "Juan Pérez",
                "juan.perez@example.com", null, CustomerStatus.ACTIVE, now, now, 1L);

        given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
        given(accountRepository.existsOpenAccountsForCustomer(customerId)).willReturn(true);

        assertThatThrownBy(() -> service.deactivate(customerId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_HAS_OPEN_ACCOUNTS);

        verify(customerRepository, never()).save(customer);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el cliente no existe al buscar por ID")
    void shouldThrowNotFoundWhenCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        given(customerRepository.findById(customerId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);
    }
}
