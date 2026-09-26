package com.switchtx.infrastructure.filter;

import com.switchtx.infrastructure.adapter.in.rest.common.ApiHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Asigna un identificador de correlación a cada petición (lo toma del header {@code X-Correlation-Id}
 * o lo genera), lo publica en el contexto de Log4j2 para que aparezca en cada línea de log y lo devuelve
 * en la respuesta. También registra un access log con método, ruta, estado y duración.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "correlationId";

    private static final Logger log = LogManager.getLogger(CorrelationIdFilter.class);
    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(ApiHeaders.CORRELATION_ID));
        long start = System.nanoTime();
        ThreadContext.put(MDC_KEY, correlationId);
        response.setHeader(ApiHeaders.CORRELATION_ID, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(),
                    elapsedMs);
            ThreadContext.remove(MDC_KEY); // los hilos del servidor se reutilizan: evitar fugas de contexto
        }
    }

    /** Solo se acepta un id entrante con formato seguro para evitar inyección en los logs. */
    private static String resolveCorrelationId(String incoming) {
        return incoming != null && SAFE_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
