package com.switchtx.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchtx.application.port.in.account.AccountCommandUseCase;
import com.switchtx.application.port.in.account.AccountQueryUseCase;
import com.switchtx.application.port.in.account.OpenAccountCommand;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.domain.model.account.AccountType;
import com.switchtx.domain.model.shared.Money;
import com.switchtx.infrastructure.adapter.in.rest.account.AccountController;
import com.switchtx.infrastructure.adapter.in.rest.account.OpenAccountRequest;
import com.switchtx.infrastructure.adapter.in.rest.account.UpdateAccountStatusRequest;
import com.switchtx.infrastructure.adapter.in.rest.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccountController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountCommandUseCase commands;

    @MockitoBean
    private AccountQueryUseCase queries;

    @MockitoBean
    private com.switchtx.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository customerJpaRepository;

    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");
    private final java.util.Currency usd = java.util.Currency.getInstance("USD");

    @Test
    @DisplayName("POST /accounts - 201 Created con cabecera Location y datos de la cuenta")
    void shouldReturnCreatedWhenOpeningAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Account account = Account.restore(accountId, "1000000001", customerId, AccountType.CHECKING,
                usd, Money.zero(usd), AccountStatus.ACTIVE, now, now, 1L);

        given(commands.open(any(OpenAccountCommand.class))).willReturn(account);

        OpenAccountRequest request = new OpenAccountRequest(customerId, AccountType.CHECKING, "USD");

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/accounts/" + accountId)))
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("1000000001"))
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("POST /accounts - 404 Not Found si el cliente no existe")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        UUID customerId = UUID.randomUUID();
        OpenAccountRequest request = new OpenAccountRequest(customerId, AccountType.SAVINGS, "USD");

        given(commands.open(any(OpenAccountCommand.class)))
                .willThrow(new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "No existe el cliente"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /accounts/{id} - 200 OK cuando existe la cuenta")
    void shouldReturn200ForExistingAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Account account = Account.restore(accountId, "1000000001", customerId, AccountType.SAVINGS,
                usd, Money.of(new BigDecimal("250.00"), "USD"), AccountStatus.ACTIVE, now, now, 1L);

        given(queries.getById(accountId)).willReturn(account);

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.accountNumber").value("1000000001"))
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    @DisplayName("GET /accounts/{id} - 404 Not Found cuando no existe la cuenta")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        UUID accountId = UUID.randomUUID();

        given(queries.getById(accountId))
                .willThrow(new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "No existe la cuenta"));

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /accounts/{id}/status - 200 OK al cambiar de estado")
    void shouldReturn200WhenChangingStatus() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Account account = Account.restore(accountId, "1000000001", customerId, AccountType.SAVINGS,
                usd, Money.zero(usd), AccountStatus.BLOCKED, now, now, 1L);

        given(commands.changeStatus(accountId, AccountStatus.BLOCKED)).willReturn(account);

        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(AccountStatus.BLOCKED);

        mockMvc.perform(patch("/api/v1/accounts/{id}/status", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.toString()))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    @DisplayName("DELETE /accounts/{id} - 204 No Content cuando se cierra la cuenta")
    void shouldReturn204WhenAccountClosed() throws Exception {
        UUID accountId = UUID.randomUUID();
        willDoNothing().given(commands).close(accountId);

        mockMvc.perform(delete("/api/v1/accounts/{id}", accountId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /accounts/{id} - 422 Unprocessable Entity si la cuenta tiene saldo pendiente")
    void shouldReturn422WhenAccountHasBalance() throws Exception {
        UUID accountId = UUID.randomUUID();
        willThrow(new BusinessRuleViolationException(ErrorCode.ACCOUNT_BALANCE_NOT_ZERO,
                "La cuenta tiene saldo pendiente"))
                .given(commands).close(accountId);

        mockMvc.perform(delete("/api/v1/accounts/{id}", accountId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("ACCOUNT_BALANCE_NOT_ZERO"));
    }
}
