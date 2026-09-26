package com.switchtx.infrastructure.adapter.in.rest.account;

import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.infrastructure.adapter.in.rest.common.ApiPaths;
import com.switchtx.infrastructure.adapter.in.rest.common.PageResponse;
import com.switchtx.infrastructure.adapter.in.rest.common.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/** Contrato HTTP + documentación OpenAPI del recurso Cuentas. */
@Tag(name = "Cuentas", description = "Apertura, consulta, bloqueo y cierre de cuentas")
@RequestMapping(ApiPaths.ACCOUNTS)
public interface AccountApi {

    @Operation(summary = "Abrir cuenta", description = "El número de cuenta se genera automáticamente")
    @ApiResponse(responseCode = "201", description = "Cuenta abierta")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Cliente no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Cliente inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ACCOUNT_CREATE')")
    @PostMapping
    ResponseEntity<AccountResponse> open(@Valid @RequestBody OpenAccountRequest request);

    @Operation(summary = "Consultar cuenta por id (incluye saldo)")
    @ApiResponse(responseCode = "200", description = "Cuenta encontrada")
    @ApiResponse(responseCode = "404", description = "Cuenta no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ACCOUNT_READ')")
    @GetMapping("/{accountId}")
    AccountResponse getById(@PathVariable UUID accountId);

    @Operation(summary = "Listar cuentas", description = "Paginado, ordenado de la más reciente a la más antigua")
    @ApiResponse(responseCode = "200", description = "Página de cuentas")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ACCOUNT_READ')")
    @GetMapping
    PageResponse<AccountResponse> search(
            @Parameter(description = "Filtra por cliente titular") @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Filtra por estado") @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) @Min(1) @Max(Pagination.MAX_SIZE) int size);

    @Operation(summary = "Extracto de la cuenta", description = "Movimientos contables del más reciente al más antiguo")
    @ApiResponse(responseCode = "200", description = "Página de movimientos")
    @ApiResponse(responseCode = "404", description = "Cuenta no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ACCOUNT_READ')")
    @GetMapping("/{accountId}/movements")
    PageResponse<MovementResponse> getMovements(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) @Min(1) @Max(Pagination.MAX_SIZE) int size);

    @Operation(summary = "Bloquear o reactivar cuenta", description = "Transiciones permitidas: ACTIVE → BLOCKED y BLOCKED → ACTIVE")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "404", description = "Cuenta no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Transición de estado no permitida", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ACCOUNT_UPDATE_STATUS')")
    @PatchMapping("/{accountId}/status")
    AccountResponse changeStatus(@PathVariable UUID accountId, @Valid @RequestBody UpdateAccountStatusRequest request);

    @Operation(summary = "Cerrar cuenta", description = "Cierre definitivo. Requiere saldo cero.")
    @ApiResponse(responseCode = "204", description = "Cuenta cerrada")
    @ApiResponse(responseCode = "404", description = "Cuenta no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "La cuenta tiene saldo o ya está cerrada", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ACCOUNT_CLOSE')")
    @DeleteMapping("/{accountId}")
    ResponseEntity<Void> close(@PathVariable UUID accountId);
}
