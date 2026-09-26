package com.switchtx.infrastructure.adapter.in.rest.common;

public final class ApiHeaders {

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private ApiHeaders() {
    }
}
