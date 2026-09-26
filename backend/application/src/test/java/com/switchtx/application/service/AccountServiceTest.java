package com.switchtx.application.service;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.in.account.OpenAccountCommand;
import com.switchtx.application.port.out.AccountRepositoryPort;
import com.switchtx.application.port.out.CustomerRepositoryPort;
import com.switchtx.application.port.out.MovementRepositoryPort;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.domain.model.account.AccountType;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.domain.model.customer.DocumentType;
import com.switchtx.domain.model.shared.Money;
import com.switchtx.domain.model.transaction.Movement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepositoryPort accountRepository;

    @Mock
    private CustomerRepositoryPort customerRepository;

    @Mock
    private MovementRepositoryPort movementRepository;

    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final Currency USD = Currency.getInstance("USD");

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(accountRepository, customerRepository, movementRepository,
                new FakeUnitOfWork(), clock);
    }

    @Test
    @DisplayName("Abre cuenta exitosamente para cliente activo con saldo cero")
    void shouldOpenAccountForActiveCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.restore(customerId, DocumentType.CC, "1234567890", "Ana Gómez",
                "ana@example.com", null, CustomerStatus.ACTIVE, now, now, 1L);

        given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
        given(accountRepository.nextAccountNumber()).willReturn("1000000001");
        given(accountRepository.save(any(Account.class))).willAnswer(inv -> inv.getArgument(0));

        OpenAccountCommand command = new OpenAccountCommand(customerId, AccountType.SAVINGS, "USD");

        Account created = service.open(command);

        assertThat(created).isNotNull();
        assertThat(created.getAccountNumber()).isEqualTo("1000000001");
        assertThat(created.getCustomerId()).isEqualTo(customerId);
        assertThat(created.getCurrency()).isEqualTo(USD);
        assertThat(created.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(created.getBalance()).isEqualTo(Money.zero(USD));

        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Rechaza apertura si el cliente no existe")
    void shouldRejectOpenAccountWhenCustomerNotFound() {
        UUID customerId = UUID.randomUUID();
        given(customerRepository.findById(customerId)).willReturn(Optional.empty());

        OpenAccountCommand command = new OpenAccountCommand(customerId, AccountType.SAVINGS, "USD");

        assertThatThrownBy(() -> service.open(command))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND);

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza apertura si el cliente está inactivo")
    void shouldRejectOpenAccountWhenCustomerInactive() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.restore(customerId, DocumentType.CC, "1234567890", "Ana Gómez",
                "ana@example.com", null, CustomerStatus.INACTIVE, now, now, 1L);

        given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));

        OpenAccountCommand command = new OpenAccountCommand(customerId, AccountType.SAVINGS, "USD");

        assertThatThrownBy(() -> service.open(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CUSTOMER_INACTIVE);

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambia estado de ACTIVE a BLOCKED y de BLOCKED a ACTIVE")
    void shouldChangeStatusBetweenActiveAndBlocked() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open("1000000001", UUID.randomUUID(), AccountType.SAVINGS, USD, now);

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(accountRepository.save(account)).willReturn(account);

        Account blocked = service.changeStatus(accountId, AccountStatus.BLOCKED);
        assertThat(blocked.getStatus()).isEqualTo(AccountStatus.BLOCKED);

        Account active = service.changeStatus(accountId, AccountStatus.ACTIVE);
        assertThat(active.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Rechaza cambio de estado a CLOSED mediante changeStatus")
    void shouldRejectChangeStatusToClosed() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open("1000000001", UUID.randomUUID(), AccountType.SAVINGS, USD, now);

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> service.changeStatus(accountId, AccountStatus.CLOSED))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);

        verify(accountRepository, never()).save(account);
    }

    @Test
    @DisplayName("Cierra cuenta con saldo cero bajo bloqueo pesimista")
    void shouldCloseAccountWithZeroBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open("1000000001", UUID.randomUUID(), AccountType.SAVINGS, USD, now);

        given(accountRepository.findByIdForUpdate(accountId)).willReturn(Optional.of(account));

        service.close(accountId);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    @DisplayName("Rechaza cerrar cuenta si tiene saldo positivo")
    void shouldRejectCloseAccountWithPositiveBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open("1000000001", UUID.randomUUID(), AccountType.SAVINGS, USD, now);
        account.credit(Money.of(new BigDecimal("100.00"), "USD"), now);

        given(accountRepository.findByIdForUpdate(accountId)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> service.close(accountId))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_BALANCE_NOT_ZERO);

        verify(accountRepository, never()).save(account);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si la cuenta no existe")
    void shouldThrowNotFoundWhenAccountDoesNotExist() {
        UUID accountId = UUID.randomUUID();
        given(accountRepository.findById(accountId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(accountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("Obtiene los movimientos paginados de una cuenta existente")
    void shouldGetMovementsForExistingAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open("1000000001", UUID.randomUUID(), AccountType.SAVINGS, USD, now);
        PageQuery pageQuery = new PageQuery(0, 10);
        PageResult<Movement> expectedResult = new PageResult<>(List.of(), 0, 10, 0, 0);

        given(accountRepository.findById(accountId)).willReturn(Optional.of(account));
        given(movementRepository.findByAccountId(accountId, pageQuery)).willReturn(expectedResult);

        PageResult<Movement> result = service.getMovements(accountId, pageQuery);

        assertThat(result).isSameAs(expectedResult);
    }
}
