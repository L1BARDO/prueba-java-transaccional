package com.switchtx.infrastructure.adapter.in.rest.transaction;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.port.in.transaction.DepositCommand;
import com.switchtx.application.port.in.transaction.DepositUseCase;
import com.switchtx.application.port.in.transaction.TransactionFilter;
import com.switchtx.application.port.in.transaction.TransactionQueryUseCase;
import com.switchtx.application.port.in.transaction.TransactionResult;
import com.switchtx.application.port.in.transaction.TransferCommand;
import com.switchtx.application.port.in.transaction.TransferUseCase;
import com.switchtx.application.port.in.transaction.WithdrawalCommand;
import com.switchtx.application.port.in.transaction.WithdrawalUseCase;
import com.switchtx.domain.model.transaction.TransactionStatus;
import com.switchtx.domain.model.transaction.TransactionType;
import com.switchtx.infrastructure.adapter.in.rest.common.ApiHeaders;
import com.switchtx.infrastructure.adapter.in.rest.common.ApiPaths;
import com.switchtx.infrastructure.adapter.in.rest.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class TransactionController implements TransactionApi {

    private final DepositUseCase depositUseCase;
    private final WithdrawalUseCase withdrawalUseCase;
    private final TransferUseCase transferUseCase;
    private final TransactionQueryUseCase queries;

    public TransactionController(DepositUseCase depositUseCase, WithdrawalUseCase withdrawalUseCase,
                                 TransferUseCase transferUseCase, TransactionQueryUseCase queries) {
        this.depositUseCase = depositUseCase;
        this.withdrawalUseCase = withdrawalUseCase;
        this.transferUseCase = transferUseCase;
        this.queries = queries;
    }

    @Override
    @PreAuthorize("hasAuthority('TRANSACTION_DEPOSIT')")
    public ResponseEntity<TransactionResponse> deposit(String idempotencyKey, DepositRequest request) {
        return toResponse(depositUseCase.deposit(new DepositCommand(request.accountId(), request.amount(),
                request.currency(), request.description(), idempotencyKey)));
    }

    @Override
    @PreAuthorize("hasAuthority('TRANSACTION_WITHDRAW')")
    public ResponseEntity<TransactionResponse> withdraw(String idempotencyKey, WithdrawalRequest request) {
        return toResponse(withdrawalUseCase.withdraw(new WithdrawalCommand(request.accountId(), request.amount(),
                request.currency(), request.description(), idempotencyKey)));
    }

    @Override
    @PreAuthorize("hasAuthority('TRANSACTION_TRANSFER')")
    public ResponseEntity<TransactionResponse> transfer(String idempotencyKey, TransferRequest request) {
        return toResponse(transferUseCase.transfer(new TransferCommand(request.sourceAccountId(),
                request.destinationAccountId(), request.amount(), request.currency(), request.description(),
                idempotencyKey)));
    }

    @Override
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public TransactionResponse getById(UUID transactionId) {
        return TransactionResponse.from(queries.getById(transactionId));
    }

    @Override
    @PreAuthorize("hasAuthority('TRANSACTION_READ')")
    public PageResponse<TransactionResponse> search(UUID accountId, TransactionType type, TransactionStatus status,
                                                    Instant from, Instant to, int page, int size) {
        return PageResponse.from(
                queries.search(new TransactionFilter(accountId, type, status, from, to), new PageQuery(page, size)),
                TransactionResponse::from);
    }

    /** 201 + Location para operaciones nuevas; 200 + Idempotent-Replayed para reintentos. */
    private static ResponseEntity<TransactionResponse> toResponse(TransactionResult result) {
        TransactionResponse body = TransactionResponse.from(result.transaction());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ApiPaths.TRANSACTIONS + "/{id}").buildAndExpand(body.id()).toUri();
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .location(location)
                .header(ApiHeaders.IDEMPOTENT_REPLAYED, String.valueOf(result.replayed()))
                .body(body);
    }
}
