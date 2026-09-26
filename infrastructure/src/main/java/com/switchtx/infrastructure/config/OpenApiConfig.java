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

                                ### 🔐 Guía de Autenticación y Autorización
                                1. Inicie sesión en `POST /api/v1/auth/login` con uno de los usuarios de prueba.
                                2. Copie el valor del campo `token` de la respuesta JSON.
                                3. Haga clic en el botón verde **Authorize 🔓** (arriba a la derecha), pegue el token en `Value:` y confirme.

                                ### 👥 Usuarios y Roles (RBAC Parametrizado en BD)
                                * **`admin` / `Admin123*`** (`ADMIN`): Acceso y privilegios totales.
                                * **`operador` / `Operador123*`** (`OPERATOR`): Gestión de clientes, cuentas y operaciones monetarias.
                                * **`auditor` / `Auditor123*`** (`AUDITOR`): Solo lectura (`*_READ`) y reportes (`403 Forbidden` en escrituras).
                                * **`ana.perez` / `Cliente123*`** (`CUSTOMER`): Consulta de sus cuentas y transferencias (`403 Forbidden` al crear clientes/cuentas).
                                * **`bloqueado` / `Bloqueado123*`**: Cuenta bloqueada por intentos fallidos (`403 USER_LOCKED`).

                                ### ⚙️ Estándares
                                * **RFC 9457**: Respuestas de error estructuradas con `code`, `timestamp` y `correlationId`.
                                * **Idempotencia**: Header `Idempotency-Key` en depósitos, retiros y transferencias.""")
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
