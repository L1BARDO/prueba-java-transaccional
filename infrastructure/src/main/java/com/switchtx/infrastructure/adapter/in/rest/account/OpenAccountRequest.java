package com.switchtx.infrastructure.adapter.in.rest.account;

import com.switchtx.domain.model.account.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(description = "Datos para abrir una cuenta. Se abre con saldo cero y estado ACTIVE.")
public record OpenAccountRequest(
        @Schema(description = "Id del cliente titular") @NotNull UUID customerId,
        @Schema(description = "Tipo de cuenta", example = "SAVINGS") @NotNull AccountType accountType,
        @Schema(description = "Moneda ISO-4217", example = "COP")
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "debe ser un código ISO-4217 de 3 letras mayúsculas")
        String currency) {
}
