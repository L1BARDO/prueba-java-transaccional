package com.switchtx.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchtx.application.port.in.customer.CustomerCommandUseCase;
import com.switchtx.application.port.in.customer.CustomerQueryUseCase;
import com.switchtx.application.port.in.customer.RegisterCustomerCommand;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.DocumentType;
import com.switchtx.infrastructure.adapter.in.rest.customer.CreateCustomerRequest;
import com.switchtx.infrastructure.adapter.in.rest.customer.CustomerController;
import com.switchtx.infrastructure.adapter.in.rest.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(controllers = CustomerController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerCommandUseCase commands;

    @MockitoBean
    private CustomerQueryUseCase queries;

    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");

    @Test
    @DisplayName("POST /customers - 201 Created con cabecera Location y datos del cliente")
    void shouldReturnCreatedWhenRegisteringCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.restore(customerId, DocumentType.CC, "1020304050", "Carlos Ruiz",
                "carlos@example.com", "+573001234567", com.switchtx.domain.model.customer.CustomerStatus.ACTIVE,
                now, now, 1L);

        given(commands.register(any(RegisterCustomerCommand.class))).willReturn(customer);

        CreateCustomerRequest request = new CreateCustomerRequest(DocumentType.CC, "1020304050",
                "Carlos Ruiz", "carlos@example.com", "+573001234567");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/customers/" + customerId)))
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.documentType").value("CC"))
                .andExpect(jsonPath("$.documentNumber").value("1020304050"))
                .andExpect(jsonPath("$.fullName").value("Carlos Ruiz"))
                .andExpect(jsonPath("$.email").value("carlos@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /customers - 400 Bad Request cuando el payload tiene errores de validación")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Documento muy corto y email inválido
        String invalidJson = """
                {
                  "documentType": "CC",
                  "documentNumber": "12",
                  "fullName": "",
                  "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /customers - 409 Conflict cuando el documento o email ya existen")
    void shouldReturn409WhenCustomerAlreadyExists() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(DocumentType.CC, "1020304050",
                "Carlos Ruiz", "carlos@example.com", null);

        given(commands.register(any(RegisterCustomerCommand.class)))
                .willThrow(new ConflictException(ErrorCode.CUSTOMER_ALREADY_EXISTS, "Ya existe un cliente con ese documento"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CUSTOMER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.title").value(ErrorCode.CUSTOMER_ALREADY_EXISTS.defaultMessage()))
                .andExpect(jsonPath("$.detail").value("Ya existe un cliente con ese documento"));
    }

    @Test
    @DisplayName("GET /customers/{id} - 200 OK cuando existe el cliente")
    void shouldReturn200ForExistingCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.restore(customerId, DocumentType.CC, "1020304050", "Carlos Ruiz",
                "carlos@example.com", null, com.switchtx.domain.model.customer.CustomerStatus.ACTIVE,
                now, now, 1L);

        given(queries.getById(customerId)).willReturn(customer);

        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.fullName").value("Carlos Ruiz"));
    }

    @Test
    @DisplayName("GET /customers/{id} - 404 Not Found cuando el cliente no existe")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        UUID customerId = UUID.randomUUID();

        given(queries.getById(customerId))
                .willThrow(new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND, "No existe el cliente"));

        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value(ErrorCode.CUSTOMER_NOT_FOUND.defaultMessage()));
    }

    @Test
    @DisplayName("DELETE /customers/{id} - 204 No Content cuando se desactiva con éxito")
    void shouldReturn204WhenDeactivated() throws Exception {
        UUID customerId = UUID.randomUUID();
        willDoNothing().given(commands).deactivate(customerId);

        mockMvc.perform(delete("/api/v1/customers/{id}", customerId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /customers/{id} - 422 Unprocessable Entity si el cliente tiene cuentas abiertas")
    void shouldReturn422WhenCustomerHasOpenAccounts() throws Exception {
        UUID customerId = UUID.randomUUID();
        willThrow(new BusinessRuleViolationException(ErrorCode.CUSTOMER_HAS_OPEN_ACCOUNTS,
                "El cliente tiene cuentas abiertas"))
                .given(commands).deactivate(customerId);

        mockMvc.perform(delete("/api/v1/customers/{id}", customerId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("CUSTOMER_HAS_OPEN_ACCOUNTS"))
                .andExpect(jsonPath("$.title").value(ErrorCode.CUSTOMER_HAS_OPEN_ACCOUNTS.defaultMessage()))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }
}
