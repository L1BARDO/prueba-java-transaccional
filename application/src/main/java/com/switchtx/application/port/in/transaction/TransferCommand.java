package com.switchtx.application.port.in.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount,
                              String currency, String description, String idempotencyKey) {
}
