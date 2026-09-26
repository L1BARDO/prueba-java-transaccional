package com.switchtx.infrastructure.adapter.in.rest.auth;

import com.switchtx.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Detalle del usuario autenticado actual")
public record CurrentUserResponse(
        UUID userId,
        String username,
        String email,
        String fullName,
        UUID customerId,
        Set<String> roles,
        Set<String> permissions) {

    public static CurrentUserResponse from(UserPrincipal principal) {
        return new CurrentUserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getEmail(),
                principal.getFullName(),
                principal.getCustomerId(),
                principal.getRoles(),
                principal.getPermissions()
        );
    }
}
