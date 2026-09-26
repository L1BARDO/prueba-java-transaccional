package com.switchtx.infrastructure.adapter.in.rest.auth;

import com.switchtx.application.port.in.auth.AuthenticatedUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Respuesta de autenticación con token JWT")
public record LoginResponse(
        @Schema(description = "Token JWT firmado") String token,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Tiempo de validez en segundos", example = "86400") long expiresInSeconds,
        UUID userId,
        @Schema(example = "operador") String username,
        @Schema(example = "Operador de caja") String fullName,
        @Schema(example = "operador@switchtx.co") String email,
        UUID customerId,
        @Schema(description = "Roles asignados") Set<String> roles,
        @Schema(description = "Permisos efectivos") Set<String> permissions) {

    public static LoginResponse of(String token, long expiresInSeconds, AuthenticatedUser user) {
        return new LoginResponse(
                token,
                "Bearer",
                expiresInSeconds,
                user.id(),
                user.username(),
                user.fullName(),
                user.email(),
                user.customerId(),
                user.roles(),
                user.permissions()
        );
    }
}
