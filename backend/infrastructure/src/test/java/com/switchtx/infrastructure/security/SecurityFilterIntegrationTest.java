package com.switchtx.infrastructure.security;

import com.switchtx.application.port.in.customer.CustomerCommandUseCase;
import com.switchtx.application.port.in.customer.CustomerQueryUseCase;
import com.switchtx.infrastructure.adapter.in.rest.customer.CustomerController;
import com.switchtx.infrastructure.adapter.in.rest.error.GlobalExceptionHandler;
import com.switchtx.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.domain.model.customer.DocumentType;

import java.time.Instant;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
        "security.jwt.expiration-seconds=86400",
        "security.jwt.issuer=switch-transaccional"
})
class SecurityFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerCommandUseCase commands;

    @MockitoBean
    private CustomerQueryUseCase queries;

    @MockitoBean
    private JwtTokenProvider tokenProvider;

    @MockitoBean
    private com.switchtx.infrastructure.adapter.out.persistence.repository.UserJpaRepository userJpaRepository;

    @MockitoBean
    private com.switchtx.infrastructure.adapter.out.persistence.repository.RoleJpaRepository roleJpaRepository;

    @MockitoBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Petición sin token a endpoint protegido devuelve 401 con ProblemDetail RFC 9457")
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Petición con token que no posee el permiso requerido devuelve 403 Forbidden")
    void shouldReturn403WhenTokenLacksPermission() throws Exception {
        // Usuario autenticado pero solo con rol CUSTOMER y sin permiso CUSTOMER_READ
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "cliente", "cliente@mail.com",
                "Cliente", UUID.randomUUID(), Set.of("CUSTOMER"), Set.of("ACCOUNT_READ"));

        given(tokenProvider.extractUserPrincipal("valid.jwt.token")).willReturn(Optional.of(principal));

        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Petición con token válido y permiso CUSTOMER_READ devuelve 200 OK")
    void shouldReturn200WhenTokenHasRequiredPermission() throws Exception {
        UUID customerId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "operador", "operador@mail.com",
                "Operador", null, Set.of("OPERATOR"), Set.of("CUSTOMER_READ"));

        Customer customer = Customer.restore(customerId, DocumentType.CC, "123456789", "Juan Gomez",
                "juan@mail.com", null, CustomerStatus.ACTIVE, Instant.now(), Instant.now(), 1L);

        given(tokenProvider.extractUserPrincipal("valid.jwt.token")).willReturn(Optional.of(principal));
        given(queries.getById(customerId)).willReturn(customer);

        mockMvc.perform(get("/api/v1/customers/{id}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.fullName").value("Juan Gomez"));
    }
}
