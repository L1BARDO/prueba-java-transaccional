package com.switchtx.infrastructure.adapter.in.rest.account;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.port.in.account.AccountCommandUseCase;
import com.switchtx.application.port.in.account.AccountFilter;
import com.switchtx.application.port.in.account.AccountQueryUseCase;
import com.switchtx.application.port.in.account.OpenAccountCommand;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.infrastructure.adapter.in.rest.common.PageResponse;
import com.switchtx.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.switchtx.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final AccountCommandUseCase commands;
    private final AccountQueryUseCase queries;
    private final CustomerJpaRepository customerJpaRepository;

    @Override
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE')")
    public ResponseEntity<AccountResponse> open(OpenAccountRequest request) {
        Account account = commands.open(new OpenAccountCommand(request.customerId(), request.accountType(),
                request.currency()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(account.getId()).toUri();
        return ResponseEntity.created(location).body(AccountResponse.from(account));
    }

    @Override
    @PreAuthorize("hasAuthority('ACCOUNT_READ')")
    public AccountResponse getById(UUID accountId) {
        Account a = queries.getById(accountId);
        return customerJpaRepository.findById(a.getCustomerId())
                .map(c -> AccountResponse.from(a, c.getFullName(), c.getDocumentNumber()))
                .orElseGet(() -> AccountResponse.from(a));
    }

    @Override
    @PreAuthorize("hasAuthority('ACCOUNT_READ')")
    public PageResponse<AccountResponse> search(UUID customerId, AccountStatus status, int page, int size) {
        var pageResult = queries.search(new AccountFilter(customerId, status), new PageQuery(page, size));
        Set<UUID> customerIds = pageResult.content().stream().map(Account::getCustomerId).collect(Collectors.toSet());
        Map<UUID, CustomerEntity> customerMap = customerJpaRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(CustomerEntity::getId, Function.identity()));

        return PageResponse.from(pageResult, a -> {
            CustomerEntity c = customerMap.get(a.getCustomerId());
            return c != null
                    ? AccountResponse.from(a, c.getFullName(), c.getDocumentNumber())
                    : AccountResponse.from(a);
        });
    }

    @Override
    @PreAuthorize("hasAuthority('ACCOUNT_READ')")
    public PageResponse<MovementResponse> getMovements(UUID accountId, int page, int size) {
        return PageResponse.from(queries.getMovements(accountId, new PageQuery(page, size)), MovementResponse::from);
    }

    @Override
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE_STATUS')")
    public AccountResponse changeStatus(UUID accountId, UpdateAccountStatusRequest request) {
        return AccountResponse.from(commands.changeStatus(accountId, request.status()));
    }

    @Override
    @PreAuthorize("hasAuthority('ACCOUNT_CLOSE')")
    public ResponseEntity<Void> close(UUID accountId) {
        commands.close(accountId);
        return ResponseEntity.noContent().build();
    }
}
