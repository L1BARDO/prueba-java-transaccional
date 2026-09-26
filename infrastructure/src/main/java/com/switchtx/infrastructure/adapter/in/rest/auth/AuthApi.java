package com.switchtx.infrastructure.adapter.in.rest.auth;

import com.switchtx.infrastructure.adapter.in.rest.common.ApiPaths;
import com.switchtx.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Autenticación", description = "Inicio de sesión y obtención de tokens JWT")
@RequestMapping(ApiPaths.AUTH)
public interface AuthApi {

    @Operation(summary = "Iniciar sesión y obtener token JWT")
    @ApiResponse(responseCode = "200", description = "Autenticación exitosa")
    @ApiResponse(responseCode = "400", description = "Datos requeridos faltantes", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario bloqueado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request);

    @Operation(summary = "Obtener información del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Datos del usuario actual")
    @ApiResponse(responseCode = "401", description = "No autenticado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/me")
    CurrentUserResponse getCurrentUser(@AuthenticationPrincipal UserPrincipal principal);
}
