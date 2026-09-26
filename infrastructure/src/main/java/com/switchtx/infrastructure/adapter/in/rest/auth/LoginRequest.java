package com.switchtx.infrastructure.adapter.in.rest.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(
        @Schema(description = "Nombre de usuario", example = "operador")
        @NotBlank String username,

        @Schema(description = "Contraseña en texto plano", example = "Operador123*")
        @NotBlank String password) {
}
