package com.switchtx.application.port.out;

import com.switchtx.domain.model.user.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    Optional<User> findByUsername(String username);

    void updateLastLogin(UUID userId, Instant loginAt);

    void recordFailedLogin(String username, int maxAttempts);

    void resetFailedAttempts(UUID userId);
}
