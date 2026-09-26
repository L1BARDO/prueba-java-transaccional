package com.switchtx.infrastructure.adapter.out.reference;

import com.switchtx.application.port.out.TransactionReferenceGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/** Genera referencias del tipo {@code TRX-20260926173045-9F3A1C7B} (fecha UTC + 32 bits aleatorios). */
@Component
public class RandomTransactionReferenceGenerator implements TransactionReferenceGenerator {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public RandomTransactionReferenceGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String next() {
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        return "TRX-" + TIMESTAMP.format(clock.instant()) + "-" + HexFormat.of().withUpperCase().formatHex(bytes);
    }
}
