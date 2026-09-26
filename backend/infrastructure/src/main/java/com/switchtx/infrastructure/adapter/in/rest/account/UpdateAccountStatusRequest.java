package com.switchtx.infrastructure.adapter.in.rest.account;

import com.switchtx.domain.model.account.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo estado de la cuenta: BLOCKED para bloquear, ACTIVE para reactivar")
public record UpdateAccountStatusRequest(
        @Schema(example = "BLOCKED", allowableValues = {"ACTIVE", "BLOCKED"}) @NotNull AccountStatus status) {
}
