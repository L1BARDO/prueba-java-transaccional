package com.switchtx.infrastructure.adapter.in.rest.error;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.DomainException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.InvalidDataException;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.infrastructure.filter.CorrelationIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Traduce todas las excepciones a respuestas RFC 9457 (application/problem+json) con un formato uniforme:
 * {@code code}, {@code timestamp}, {@code correlationId} y, en errores de validación, {@code errors}.
 *
 * <ul>
 *   <li>{@link InvalidDataException} → 400</li>
 *   <li>{@link ResourceNotFoundException} → 404</li>
 *   <li>{@link ConflictException}, bloqueo optimista / integridad → 409</li>
 *   <li>{@link BusinessRuleViolationException} → 422</li>
 *   <li>BD no disponible → 503 · cualquier otro error → 500 (sin exponer detalles internos)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------ Dominio

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ProblemDetail> handleInvalidData(InvalidDataException ex) {
        HttpStatus status = ex.getErrorCode() == ErrorCode.INVALID_CREDENTIALS
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        return domainProblem(status, ex);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        return domainProblem(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        return domainProblem(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRule(BusinessRuleViolationException ex) {
        HttpStatus status = (ex.getErrorCode() == ErrorCode.USER_LOCKED || ex.getErrorCode() == ErrorCode.USER_DISABLED)
                ? HttpStatus.FORBIDDEN
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return domainProblem(status, ex);
    }

    // ------------------------------------------------------------------ Seguridad

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                problem(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "No cuenta con los permisos necesarios para realizar esta operación."));
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
        log.warn("Autenticación fallida: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                problem(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Se requiere autenticación para acceder a este recurso."));
    }

    // ------------------------------------------------------------------ Validación de parámetros

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> errors = ex.getConstraintViolations().stream()
                .map(v -> fieldError(v.getPropertyPath().toString(), v.getMessage(), v.getInvalidValue()))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage());
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "El parámetro '%s' tiene un valor inválido: '%s'".formatted(ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, detail));
    }

    // ------------------------------------------------------------------ Persistencia / concurrencia

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia optimista: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem(HttpStatus.CONFLICT,
                ErrorCode.CONCURRENCY_CONFLICT, "El recurso fue modificado por otra operación; consulte y reintente"));
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    public ResponseEntity<ProblemDetail> handlePessimisticLock(RuntimeException ex) {
        log.warn("No se obtuvo el bloqueo de la cuenta a tiempo: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem(HttpStatus.CONFLICT,
                ErrorCode.CONCURRENCY_CONFLICT, "La cuenta está siendo procesada por otra operación; reintente"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Violación de integridad no controlada", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem(HttpStatus.CONFLICT,
                ErrorCode.DATA_INTEGRITY_VIOLATION, ErrorCode.DATA_INTEGRITY_VIOLATION.defaultMessage()));
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ProblemDetail> handleDatabaseUnavailable(RuntimeException ex) {
        log.error("Base de datos no disponible", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem(HttpStatus.SERVICE_UNAVAILABLE,
                ErrorCode.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE.defaultMessage()));
    }

    // ------------------------------------------------------------------ Fallback

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.internalServerError().body(problem(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR, "Ocurrió un error inesperado. Reporte el correlationId a soporte."));
    }

    // ------------------------------------------------------------------ Excepciones estándar de Spring MVC

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::fieldError)
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage());
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(@NonNull HandlerMethodValidationException ex,
                                                                            @NonNull HttpHeaders headers,
                                                                            @NonNull HttpStatusCode status,
                                                                            @NonNull WebRequest request) {
        List<Map<String, Object>> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> error instanceof FieldError fieldError
                                ? fieldError(fieldError)
                                : fieldError(result.getMethodParameter().getParameterName(),
                                        error.getDefaultMessage(), result.getArgument())))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                ErrorCode.VALIDATION_ERROR.defaultMessage());
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        log.debug("Cuerpo de la petición ilegible: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "El cuerpo de la petición no es un JSON válido o contiene valores no permitidos"));
    }

    /** Enriquece también las respuestas estándar de Spring (404 de ruta, 405, 415, header faltante...). */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, @NonNull HttpHeaders headers,
                                                          @NonNull HttpStatusCode statusCode,
                                                          @NonNull WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            ErrorCode code = switch (statusCode.value()) {
                case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
                case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
                default -> statusCode.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_REQUEST;
            };
            enrich(problem, code);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    // ------------------------------------------------------------------ Helpers

    private ResponseEntity<ProblemDetail> domainProblem(HttpStatus status, DomainException ex) {
        if (status.is5xxServerError()) {
            log.error("Error de dominio {}", ex.getErrorCode(), ex);
        } else {
            log.info("Solicitud rechazada status={} code={} detail={}", status.value(), ex.getErrorCode(),
                    ex.getMessage());
        }
        return ResponseEntity.status(status).body(problem(status, ex.getErrorCode(), ex.getMessage()));
    }

    private static ProblemDetail problem(HttpStatus status, ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(code.defaultMessage());
        enrich(problem, code);
        return problem;
    }

    private static void enrich(ProblemDetail problem, ErrorCode code) {
        if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
            problem.setProperty("code", code.name());
        }
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", ThreadContext.get(CorrelationIdFilter.MDC_KEY));
    }

    private static Map<String, Object> fieldError(FieldError error) {
        return fieldError(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }

    private static Map<String, Object> fieldError(String field, String message, Object rejected) {
        return Map.of(
                "field", field == null ? "" : field,
                "message", message == null ? "valor inválido" : message,
                "rejectedValue", rejected == null ? "null" : String.valueOf(rejected));
    }
}
