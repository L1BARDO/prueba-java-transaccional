package com.switchtx.application.service;

import com.switchtx.application.port.in.auth.AuthenticatedUser;
import com.switchtx.application.port.in.auth.LoginCommand;
import com.switchtx.application.port.out.PasswordEncoderPort;
import com.switchtx.application.port.out.UserRepositoryPort;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.InvalidDataException;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.user.Permission;
import com.switchtx.domain.model.user.Role;
import com.switchtx.domain.model.user.User;
import com.switchtx.domain.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, new FakeUnitOfWork(), clock);
    }

    @Test
    @DisplayName("Autenticación exitosa con credenciales correctas")
    void shouldAuthenticateSuccessfully() {
        UUID userId = UUID.randomUUID();
        Role operatorRole = new Role(1L, "OPERATOR", "Operador", null, true,
                Set.of(new Permission(1L, "TRANSACTION_DEPOSIT", "TRANSACTION", "Depósitos")));
        User user = new User(userId, "operador", "operador@switchtx.co", "$2a$10$hash",
                "Operador", null, UserStatus.ACTIVE, 0, null, now, now, now, 0L, Set.of(operatorRole));

        given(userRepository.findByUsername("operador")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("Operador123*", "$2a$10$hash")).willReturn(true);

        LoginCommand command = new LoginCommand("operador", "Operador123*");

        AuthenticatedUser result = authService.authenticate(command);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("operador");
        assertThat(result.roles()).containsExactly("OPERATOR");
        assertThat(result.permissions()).containsExactly("TRANSACTION_DEPOSIT");

        verify(userRepository).resetFailedAttempts(userId);
        verify(userRepository).updateLastLogin(userId, now);
    }

    @Test
    @DisplayName("Falla cuando el usuario no existe con INVALID_CREDENTIALS")
    void shouldFailWhenUserNotFound() {
        given(userRepository.findByUsername("desconocido")).willReturn(Optional.empty());

        LoginCommand command = new LoginCommand("desconocido", "clave");

        assertThatThrownBy(() -> authService.authenticate(command))
                .isInstanceOf(InvalidDataException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Falla cuando la contraseña no coincide y registra intento fallido")
    void shouldFailWhenPasswordDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "operador", "operador@switchtx.co", "$2a$10$hash",
                "Operador", null, UserStatus.ACTIVE, 0, null, now, now, now, 0L, Set.of());

        given(userRepository.findByUsername("operador")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("clave_errada", "$2a$10$hash")).willReturn(false);

        LoginCommand command = new LoginCommand("operador", "clave_errada");

        assertThatThrownBy(() -> authService.authenticate(command))
                .isInstanceOf(InvalidDataException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(userRepository).recordFailedLogin("operador", 5);
    }

    @Test
    @DisplayName("Rechaza autenticación si el usuario está bloqueado (LOCKED)")
    void shouldRejectWhenUserIsLocked() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "bloqueado", "bloqueado@switchtx.co", "$2a$10$hash",
                "Bloqueado", null, UserStatus.LOCKED, 5, null, now, now, now, 0L, Set.of());

        given(userRepository.findByUsername("bloqueado")).willReturn(Optional.of(user));

        LoginCommand command = new LoginCommand("bloqueado", "clave");

        assertThatThrownBy(() -> authService.authenticate(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_LOCKED);
    }

    @Test
    @DisplayName("Obtiene el usuario actual o lanza USER_NOT_FOUND si no existe")
    void shouldGetCurrentUserOrThrow() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "admin", "admin@switchtx.co", "$2a$10$hash",
                "Admin", null, UserStatus.ACTIVE, 0, null, now, now, now, 0L, Set.of());

        given(userRepository.findByUsername("admin")).willReturn(Optional.of(user));
        given(userRepository.findByUsername("fantasma")).willReturn(Optional.empty());

        AuthenticatedUser found = authService.getCurrentUser("admin");
        assertThat(found.username()).isEqualTo("admin");

        assertThatThrownBy(() -> authService.getCurrentUser("fantasma"))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
