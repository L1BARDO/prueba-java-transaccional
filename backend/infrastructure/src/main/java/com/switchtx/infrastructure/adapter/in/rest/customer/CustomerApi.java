package com.switchtx.infrastructure.adapter.in.rest.customer;

import com.switchtx.domain.model.customer.CustomerStatus;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/** Contrato HTTP + documentación OpenAPI del recurso Clientes. */
@Tag(name = "Clientes", description = "CRUD de titulares de cuentas")
@RequestMapping(ApiPaths.CUSTOMERS)
public interface CustomerApi {

    @Operation(summary = "Registrar cliente")
    @ApiResponse(responseCode = "201", description = "Cliente creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Documento o correo ya registrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    @PostMapping
    ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request);

    @Operation(summary = "Consultar cliente por id")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @GetMapping("/{customerId}")
    CustomerResponse getById(@PathVariable UUID customerId);

    @Operation(summary = "Listar clientes", description = "Paginado, ordenado del más reciente al más antiguo")
    @ApiResponse(responseCode = "200", description = "Página de clientes")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @GetMapping
    PageResponse<CustomerResponse> search(
            @Parameter(description = "Filtra por estado") @RequestParam(required = false) CustomerStatus status,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) @Min(1) @Max(Pagination.MAX_SIZE) int size);

    @Operation(summary = "Actualizar datos de contacto del cliente")
    @ApiResponse(responseCode = "200", description = "Cliente actualizado")
    @ApiResponse(responseCode = "404", description = "Cliente no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Correo ya registrado o conflicto de concurrencia", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Cliente inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @PutMapping("/{customerId}")
    CustomerResponse update(@PathVariable UUID customerId, @Valid @RequestBody UpdateCustomerRequest request);

    @Operation(summary = "Inactivar cliente (baja lógica)", description = "Requiere que el cliente no tenga cuentas abiertas")
    @ApiResponse(responseCode = "204", description = "Cliente inactivado")
    @ApiResponse(responseCode = "404", description = "Cliente no existe", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Tiene cuentas abiertas o ya está inactivo", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_DELETE')")
    @DeleteMapping("/{customerId}")
    ResponseEntity<Void> deactivate(@PathVariable UUID customerId);
}
