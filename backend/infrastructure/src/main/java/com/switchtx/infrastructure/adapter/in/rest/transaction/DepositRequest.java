package com.switchtx.infrastructure.adapter.in.rest.transaction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Depósito (consignación) a una cuenta")
public record DepositRequest(
        @Schema(description = "Cuenta destino") @NotNull UUID accountId,
        @Schema(example = "150000.00")
        @NotNull @DecimalMin(MoneyFields.MIN_AMOUNT) @Digits(integer = MoneyFields.INTEGER_DIGITS, fraction = MoneyFields.FRACTION_DIGITS)
        BigDecimal amount,
        @Schema(description = "Debe coincidir con la moneda de la cuenta", example = "COP")
        @NotBlank @Pattern(regexp = MoneyFields.CURRENCY_REGEX, message = MoneyFields.CURRENCY_MESSAGE) String currency,
        @Schema(example = "Consignación en efectivo") @Size(max = 255) String description) {
}
