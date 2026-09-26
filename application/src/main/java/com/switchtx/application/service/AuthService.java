package com.switchtx.application.service;

import com.switchtx.application.port.in.auth.AuthenticatedUser;
import com.switchtx.application.port.in.auth.CurrentUserUseCase;
import com.switchtx.application.port.in.auth.LoginCommand;
import com.switchtx.application.port.in.auth.LoginUseCase;
import com.switchtx.application.port.out.PasswordEncoderPort;
import com.switchtx.application.port.out.UnitOfWork;
import com.switchtx.application.port.out.UserRepositoryPort;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.InvalidDataException;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.util.Objects;

public class AuthService implements LoginUseCase, CurrentUserUseCase {

    private static final Logger log = LogManager.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public AuthService(UserRepositoryPort userRepository, PasswordEncoderPort passwordEncoder,
                       UnitOfWork unitOfWork, Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public AuthenticatedUser authenticate(LoginCommand command) {
        if (command.username() == null || command.username().isBlank() ||
                command.password() == null || command.password().isBlank()) {
            throw new InvalidDataException(ErrorCode.INVALID_CREDENTIALS, "Usuario y contraseña son requeridos");
        }

        String username = command.username().trim().toLowerCase();

        return unitOfWork.execute(() -> {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new InvalidDataException(ErrorCode.INVALID_CREDENTIALS, "Credenciales inválidas"));

            user.ensureCanAuthenticate();

            if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
                log.warn("Intento de login fallido para usuario {}", username);
                userRepository.recordFailedLogin(username, MAX_FAILED_ATTEMPTS);
                throw new InvalidDataException(ErrorCode.INVALID_CREDENTIALS, "Credenciales inválidas");
            }

            userRepository.resetFailedAttempts(user.getId());
            userRepository.updateLastLogin(user.getId(), clock.instant());
            log.info("Usuario {} autenticado exitosamente", username);

            return AuthenticatedUser.from(user);
        });
    }

    @Override
    public AuthenticatedUser getCurrentUser(String username) {
        return unitOfWork.executeReadOnly(() -> {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "Usuario no encontrado"));
            return AuthenticatedUser.from(user);
        });
    }
}
