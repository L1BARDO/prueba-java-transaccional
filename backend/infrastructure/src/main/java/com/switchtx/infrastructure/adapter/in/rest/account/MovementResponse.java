package com.switchtx.infrastructure.adapter.in.rest.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.switchtx.domain.model.transaction.Movement;
import com.switchtx.domain.model.transaction.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Movimiento contable de una cuenta")
public record MovementResponse(
        UUID id,
        UUID transactionId,
        MovementType movementType,
        @Schema(example = "50000.00") BigDecimal amount,
        @Schema(example = "COP") String currency,
        @Schema(description = "Saldo tras aplicar el movimiento", example = "100000.00") BigDecimal balanceAfter,
        Instant createdAt) {

    @JsonProperty("type")
    public MovementType type() {
        return movementType;
    }

    public static MovementResponse from(Movement m) {
        return new MovementResponse(m.id(), m.transactionId(), m.type(), m.amount().amount(),
                m.amount().currency().getCurrencyCode(), m.balanceAfter().amount(), m.createdAt());
    }
}
