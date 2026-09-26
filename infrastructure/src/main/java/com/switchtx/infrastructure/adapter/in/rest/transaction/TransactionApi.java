package com.switchtx.infrastructure.adapter.in.rest.transaction;

import com.switchtx.domain.model.transaction.TransactionStatus;
import com.switchtx.domain.model.transaction.TransactionType;
import com.switchtx.infrastructure.adapter.in.rest.common.ApiHeaders;
import com.switchtx.infrastructure.adapter.in.rest.common.ApiPaths;
import com.switchtx.infrastructure.adapter.in.rest.common.PageResponse;
import com.switchtx.infrastructure.adapter.in.rest.common.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.UUID;

/** Contrato HTTP + documentación OpenAPI de las operaciones monetarias. */
@Tag(name = "Transacciones", description = "Depósitos, retiros y transferencias entre cuentas")
@RequestMapping(ApiPaths.TRANSACTIONS)
public interface TransactionApi {

    String IDEMPOTENCY_DOC = "Llave única por operación. Reintentar con la misma llave devuelve el resultado "
            + "original sin volver a mover dinero.";

    @Operation(summary = "Depositar", description = "Acredita el monto en la cuenta destino")
    @ApiResponse(responseCode = "201", description = "Depósito aplicado")
    @ApiResponse(responseCode = "200", description = "Reintento idempotente: se devuelve la transacción original",
            headers = @Header(name = ApiHeaders.IDEMPOTENT_REPLAYED, description = "true"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Cuenta no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Llave de idempotencia usada con otra operación", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Rechazada: cuenta no activa o moneda distinta", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('TRANSACTION_DEPOSIT')")
    @PostMapping("/deposits")
    ResponseEntity<TransactionResponse> deposit(
            @Parameter(description = IDEMPOTENCY_DOC) @RequestHeader(name = ApiHeaders.IDEMPOTENCY_KEY, required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody DepositRequest request);

    @Operation(summary = "Retirar", description = "Debita el monto de la cuenta origen")
    @ApiResponse(responseCode = "201", description = "Retiro aplicado")
    @ApiResponse(responseCode = "200", description = "Reintento idempotente: se devuelve la transacción original",
            headers = @Header(name = ApiHeaders.IDEMPOTENT_REPLAYED, description = "true"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Cuenta no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Llave de idempotencia usada con otra operación", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Rechazada: fondos insuficientes, cuenta no activa o moneda distinta", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('TRANSACTION_WITHDRAW')")
    @PostMapping("/withdrawals")
    ResponseEntity<TransactionResponse> withdraw(
            @Parameter(description = IDEMPOTENCY_DOC) @RequestHeader(name = ApiHeaders.IDEMPOTENCY_KEY, required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody WithdrawalRequest request);

    @Operation(summary = "Transferir entre cuentas", description = "Debita la cuenta origen y acredita la destino de forma atómica")
    @ApiResponse(responseCode = "201", description = "Transferencia aplicada")
    @ApiResponse(responseCode = "200", description = "Reintento idempotente: se devuelve la transacción original",
            headers = @Header(name = ApiHeaders.IDEMPOTENT_REPLAYED, description = "true"))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Alguna cuenta no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Llave de idempotencia usada con otra operación", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Rechazada: fondos insuficientes, misma cuenta, cuenta no activa o monedas distintas", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('TRANSACTION_TRANSFER')")
    @PostMapping("/transfers")
    ResponseEntity<TransactionResponse> transfer(
            @Parameter(description = IDEMPOTENCY_DOC) @RequestHeader(name = ApiHeaders.IDEMPOTENCY_KEY, required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody TransferRequest request);

    @Operation(summary = "Consultar transacción por id")
    @ApiResponse(responseCode = "200", description = "Transacción encontrada")
    @ApiResponse(responseCode = "404", description = "Transacción no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('TRANSACTION_READ')")
    @GetMapping("/{transactionId}")
    TransactionResponse getById(@PathVariable UUID transactionId);

    @Operation(summary = "Buscar transacciones", description = "Todos los filtros son opcionales. Orden: más reciente primero.")
    @ApiResponse(responseCode = "200", description = "Página de transacciones")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('TRANSACTION_READ')")
    @GetMapping
    PageResponse<TransactionResponse> search(
            @Parameter(description = "Cuenta como origen o destino") @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Desde (ISO-8601)", example = "2026-01-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Hasta (ISO-8601)", example = "2026-12-31T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) @Min(1) @Max(Pagination.MAX_SIZE) int size);
}
