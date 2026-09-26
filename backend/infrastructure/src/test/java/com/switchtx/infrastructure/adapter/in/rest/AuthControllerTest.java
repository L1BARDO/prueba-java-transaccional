package com.switchtx.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchtx.application.port.in.auth.AuthenticatedUser;
import com.switchtx.application.port.in.auth.CurrentUserUseCase;
import com.switchtx.application.port.in.auth.LoginCommand;
import com.switchtx.application.port.in.auth.LoginUseCase;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.InvalidDataException;
import com.switchtx.infrastructure.adapter.in.rest.auth.AuthController;
import com.switchtx.infrastructure.adapter.in.rest.auth.LoginRequest;
import com.switchtx.infrastructure.adapter.in.rest.error.GlobalExceptionHandler;
import com.switchtx.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private CurrentUserUseCase currentUserUseCase;

    @MockitoBean
    private JwtTokenProvider tokenProvider;

    @Test
    @DisplayName("POST /auth/login - 200 OK con token JWT y datos del usuario")
    void shouldLoginSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "operador", "operador@switchtx.co",
                "Operador de caja", null, Set.of("OPERATOR"), Set.of("TRANSACTION_DEPOSIT", "ACCOUNT_READ"));

        given(loginUseCase.authenticate(any(LoginCommand.class))).willReturn(user);
        given(tokenProvider.generateToken(user)).willReturn("fake.jwt.token");
        given(tokenProvider.getExpirationSeconds()).willReturn(86400L);

        LoginRequest request = new LoginRequest("operador", "Operador123*");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(86400))
                .andExpect(jsonPath("$.username").value("operador"))
                .andExpect(jsonPath("$.roles[0]").value("OPERATOR"))
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    @DisplayName("POST /auth/login - 401 Unauthorized si las credenciales son incorrectas")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        given(loginUseCase.authenticate(any(LoginCommand.class)))
                .willThrow(new InvalidDataException(ErrorCode.INVALID_CREDENTIALS, "Credenciales inválidas"));

        LoginRequest request = new LoginRequest("operador", "ClaveIncorrecta");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.title").value(ErrorCode.INVALID_CREDENTIALS.defaultMessage()));
    }

    @Test
    @DisplayName("POST /auth/login - 403 Forbidden si el usuario está bloqueado")
    void shouldReturn403WhenUserIsLocked() throws Exception {
        given(loginUseCase.authenticate(any(LoginCommand.class)))
                .willThrow(new BusinessRuleViolationException(ErrorCode.USER_LOCKED,
                        "El usuario se encuentra bloqueado"));

        LoginRequest request = new LoginRequest("bloqueado", "Bloqueado123*");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("USER_LOCKED"))
                .andExpect(jsonPath("$.title").value(ErrorCode.USER_LOCKED.defaultMessage()));
    }

    @Test
    @DisplayName("POST /auth/login - 400 Bad Request si faltan datos en el body")
    void shouldReturn400WhenPayloadInvalid() throws Exception {
        String invalidJson = """
                {
                  "username": "",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
