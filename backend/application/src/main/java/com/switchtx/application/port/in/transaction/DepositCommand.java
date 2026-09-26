package com.switchtx.application.port.in.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositCommand(UUID accountId, BigDecimal amount, String currency, String description,
                             String idempotencyKey) {
}
