package com.switchtx.infrastructure.adapter.in.rest.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos actualizables del cliente (el documento no se puede modificar)")
public record UpdateCustomerRequest(
        @Schema(example = "Ana María Pérez Gómez") @NotBlank @Size(max = 150) String fullName,
        @Schema(example = "ana.perez@mail.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(example = "+573009876543")
        @Pattern(regexp = "^(\\+?[0-9\\s\\-]{7,20})?$", message = "debe tener entre 7 y 15 dígitos, opcionalmente con '+'")
        String phone,
        @Schema(description = "Nueva contraseña opcional para el acceso al portal del cliente", example = "Cliente123*")
        @Size(max = 100)
        String password) {
}
