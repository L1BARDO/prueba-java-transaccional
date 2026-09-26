package com.switchtx.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.infrastructure.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Se requiere autenticación para acceder a este recurso. Envíe un token Bearer válido."
        );
        problem.setTitle(ErrorCode.UNAUTHORIZED.defaultMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ErrorCode.UNAUTHORIZED.name());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", ThreadContext.get(CorrelationIdFilter.MDC_KEY));

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
