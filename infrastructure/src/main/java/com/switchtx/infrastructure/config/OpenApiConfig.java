package com.switchtx.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI switchOpenApi(@Value("${server.port:8080}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Switch Transaccional API")
                        .version("v1")
                        .description("""
                                API REST de un switch transaccional: gestión de clientes y cuentas, \
                                depósitos, retiros y transferencias entre cuentas con seguridad JWT.

                                * Autenticación vía `POST /api/v1/auth/login` con credenciales de usuario.
                                * Los errores siguen el estándar **RFC 9457 (Problem Details)** e incluyen \
                                `code`, `timestamp` y `correlationId`.
                                * Las operaciones monetarias aceptan el header `Idempotency-Key` para \
                                reintentos seguros.
                                * Toda respuesta incluye el header `X-Correlation-Id` para rastrear la \
                                petición en los logs.""")
                        .license(new License().name("Uso para prueba técnica")))
                .servers(List.of(new Server().url("http://localhost:" + port).description("Local")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese el token JWT obtenido en /api/v1/auth/login (sin el prefijo Bearer)")));
    }
}
