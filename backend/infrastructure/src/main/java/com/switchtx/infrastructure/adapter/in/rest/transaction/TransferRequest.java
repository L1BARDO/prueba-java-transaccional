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

@Schema(description = "Transferencia entre dos cuentas de la misma moneda")
public record TransferRequest(
        @Schema(description = "Cuenta que se debita") @NotNull UUID sourceAccountId,
        @Schema(description = "Cuenta que se acredita") @NotNull UUID destinationAccountId,
        @Schema(example = "25000.00")
        @NotNull @DecimalMin(MoneyFields.MIN_AMOUNT) @Digits(integer = MoneyFields.INTEGER_DIGITS, fraction = MoneyFields.FRACTION_DIGITS)
        BigDecimal amount,
        @Schema(example = "COP")
        @NotBlank @Pattern(regexp = MoneyFields.CURRENCY_REGEX, message = MoneyFields.CURRENCY_MESSAGE) String currency,
        @Schema(example = "Pago arriendo") @Size(max = 255) String description) {
}
