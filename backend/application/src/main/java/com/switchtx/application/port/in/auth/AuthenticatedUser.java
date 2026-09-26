package com.switchtx.application.port.in.auth;

import com.switchtx.domain.model.user.User;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(UUID id,
                                String username,
                                String email,
                                String fullName,
                                UUID customerId,
                                Set<String> roles,
                                Set<String> permissions) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getCustomerId(),
                user.getAllRoleCodes(),
                user.getAllPermissions()
        );
    }
}
