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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "No cuenta con los permisos necesarios para realizar esta operación."
        );
        problem.setTitle(ErrorCode.FORBIDDEN.defaultMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ErrorCode.FORBIDDEN.name());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", ThreadContext.get(CorrelationIdFilter.MDC_KEY));

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
