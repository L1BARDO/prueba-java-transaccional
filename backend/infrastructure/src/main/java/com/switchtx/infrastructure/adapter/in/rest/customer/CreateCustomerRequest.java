package com.switchtx.infrastructure.adapter.in.rest.customer;

import com.switchtx.domain.model.customer.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar un cliente")
public record CreateCustomerRequest(
        @Schema(description = "Tipo de documento", example = "CC")
        @NotNull DocumentType documentType,

        @Schema(description = "Número de documento (alfanumérico)", example = "1020304050")
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{5,20}$", message = "debe tener entre 5 y 20 caracteres alfanuméricos")
        String documentNumber,

        @Schema(description = "Nombre completo", example = "Ana María Pérez")
        @NotBlank @Size(max = 150) String fullName,

        @Schema(description = "Correo electrónico", example = "ana.perez@mail.com")
        @NotBlank @Email @Size(max = 150) String email,

        @Schema(description = "Teléfono (opcional)", example = "+573001234567")
        @Pattern(regexp = "^(\\+?[0-9\\s\\-]{7,20})?$", message = "debe tener entre 7 y 15 dígitos, opcionalmente con '+'")
        String phone) {
}
