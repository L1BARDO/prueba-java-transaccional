package com.switchtx.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchtx.application.port.in.transaction.DepositCommand;
import com.switchtx.application.port.in.transaction.DepositUseCase;
import com.switchtx.application.port.in.transaction.TransactionQueryUseCase;
import com.switchtx.application.port.in.transaction.TransactionResult;
import com.switchtx.application.port.in.transaction.TransferCommand;
import com.switchtx.application.port.in.transaction.TransferUseCase;
import com.switchtx.application.port.in.transaction.WithdrawalCommand;
import com.switchtx.application.port.in.transaction.WithdrawalUseCase;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.shared.Money;
import com.switchtx.domain.model.transaction.Transaction;
import com.switchtx.domain.model.transaction.TransactionDetails;
import com.switchtx.domain.model.transaction.TransactionStatus;
import com.switchtx.infrastructure.adapter.in.rest.common.ApiHeaders;
import com.switchtx.infrastructure.adapter.in.rest.error.GlobalExceptionHandler;
import com.switchtx.infrastructure.adapter.in.rest.transaction.DepositRequest;
import com.switchtx.infrastructure.adapter.in.rest.transaction.TransactionController;
import com.switchtx.infrastructure.adapter.in.rest.transaction.TransferRequest;
import com.switchtx.infrastructure.adapter.in.rest.transaction.WithdrawalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(controllers = TransactionController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DepositUseCase depositUseCase;

    @MockitoBean
    private WithdrawalUseCase withdrawalUseCase;

    @MockitoBean
    private TransferUseCase transferUseCase;

    @MockitoBean
    private TransactionQueryUseCase transactionQueryUseCase;

    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    @DisplayName("POST /deposits - 201 Created para depósito nuevo con cabecera Location e Idempotent-Replayed=false")
    void shouldReturnCreatedForNewDeposit() throws Exception {
        UUID accountId = UUID.randomUUID();
        TransactionDetails details = TransactionDetails.deposit(accountId, Money.of(new BigDecimal("150.00"), "USD"),
                "Depósito", "key-1");
        Transaction tx = Transaction.completed(details, "TX-DEP-100", now);

        given(depositUseCase.deposit(any(DepositCommand.class)))
                .willReturn(TransactionResult.created(tx));

        DepositRequest request = new DepositRequest(accountId, new BigDecimal("150.00"), "USD", "Depósito");

        mockMvc.perform(post("/api/v1/transactions/deposits")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(ApiHeaders.IDEMPOTENT_REPLAYED, "false"))
                .andExpect(header().string("Location", containsString("/api/v1/transactions/" + tx.getId())))
                .andExpect(jsonPath("$.id").value(tx.getId().toString()))
                .andExpect(jsonPath("$.reference").value("TX-DEP-100"))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("POST /deposits - 200 OK para replay idempotente con cabecera Idempotent-Replayed=true")
    void shouldReturnOkForReplayedDeposit() throws Exception {
        UUID accountId = UUID.randomUUID();
        TransactionDetails details = TransactionDetails.deposit(accountId, Money.of(new BigDecimal("100.00"), "USD"),
                "Depósito", "key-replay");
        Transaction tx = Transaction.completed(details, "TX-DEP-PREV", now);

        given(depositUseCase.deposit(any(DepositCommand.class)))
                .willReturn(TransactionResult.replayed(tx));

        DepositRequest request = new DepositRequest(accountId, new BigDecimal("100.00"), "USD", "Depósito");

        mockMvc.perform(post("/api/v1/transactions/deposits")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "key-replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiHeaders.IDEMPOTENT_REPLAYED, "true"))
                .andExpect(jsonPath("$.id").value(tx.getId().toString()))
                .andExpect(jsonPath("$.reference").value("TX-DEP-PREV"));
    }

    @Test
    @DisplayName("POST /withdrawals - 422 Unprocessable Entity cuando hay fondos insuficientes con ProblemDetail")
    void shouldReturn422WhenInsufficientFunds() throws Exception {
        UUID accountId = UUID.randomUUID();
        WithdrawalRequest request = new WithdrawalRequest(accountId, new BigDecimal("500.00"), "USD", "Retiro cajero");

        given(withdrawalUseCase.withdraw(any(WithdrawalCommand.class)))
                .willThrow(new BusinessRuleViolationException(ErrorCode.INSUFFICIENT_FUNDS,
                        "La cuenta no tiene fondos suficientes"));

        mockMvc.perform(post("/api/v1/transactions/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$.title").value(ErrorCode.INSUFFICIENT_FUNDS.defaultMessage()))
                .andExpect(jsonPath("$.detail").value("La cuenta no tiene fondos suficientes"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("POST /transfers - 409 Conflict cuando hay conflicto con la llave de idempotencia")
    void shouldReturn409WhenIdempotencyKeyConflict() throws Exception {
        UUID src = UUID.randomUUID();
        UUID dst = UUID.randomUUID();
        TransferRequest request = new TransferRequest(src, dst, new BigDecimal("50.00"), "USD", "Transferencia");

        given(transferUseCase.transfer(any(TransferCommand.class)))
                .willThrow(new ConflictException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                        "La llave de idempotencia ya fue usada con otra operación"));

        mockMvc.perform(post("/api/v1/transactions/transfers")
                        .header(ApiHeaders.IDEMPOTENCY_KEY, "reused-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    @DisplayName("POST /deposits - 400 Bad Request cuando el request no pasa la validación Bean Validation")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Monto negativo y moneda inválida
        String invalidJson = """
                {
                  "accountId": "%s",
                  "amount": -50.00,
                  "currency": "INVALID",
                  "description": "test"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/transactions/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("GET /transactions/{id} - 200 OK cuando existe la transacción")
    void shouldReturn200ForExistingTransaction() throws Exception {
        UUID txId = UUID.randomUUID();
        TransactionDetails details = TransactionDetails.deposit(UUID.randomUUID(),
                Money.of(new BigDecimal("100.00"), "USD"), "Depósito", null);
        Transaction tx = Transaction.completed(details, "TX-GET-001", now);

        given(transactionQueryUseCase.getById(txId)).willReturn(tx);

        mockMvc.perform(get("/api/v1/transactions/{id}", txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("TX-GET-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /transactions/{id} - 404 Not Found cuando no existe la transacción")
    void shouldReturn404WhenTransactionNotFound() throws Exception {
        UUID txId = UUID.randomUUID();

        given(transactionQueryUseCase.getById(txId))
                .willThrow(new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND, "No existe la transacción"));

        mockMvc.perform(get("/api/v1/transactions/{id}", txId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value(ErrorCode.TRANSACTION_NOT_FOUND.defaultMessage()));
    }
}
