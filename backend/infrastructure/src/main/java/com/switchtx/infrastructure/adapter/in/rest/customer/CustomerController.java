package com.switchtx.infrastructure.adapter.in.rest.customer;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.port.in.customer.CustomerCommandUseCase;
import com.switchtx.application.port.in.customer.CustomerQueryUseCase;
import com.switchtx.application.port.in.customer.RegisterCustomerCommand;
import com.switchtx.application.port.in.customer.UpdateCustomerCommand;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.customer.CustomerStatus;
import com.switchtx.infrastructure.adapter.in.rest.common.PageResponse;
import com.switchtx.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.switchtx.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.switchtx.infrastructure.adapter.out.persistence.repository.RoleJpaRepository;
import com.switchtx.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

    private final CustomerCommandUseCase commands;
    private final CustomerQueryUseCase queries;
    private final UserJpaRepository userJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public ResponseEntity<CustomerResponse> create(CreateCustomerRequest request) {
        Customer customer = commands.register(new RegisterCustomerCommand(request.documentType(),
                request.documentNumber(), request.fullName(), request.email(), sanitizePhone(request.phone())));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(customer.getId()).toUri();
        return ResponseEntity.created(location).body(CustomerResponse.from(customer));
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public CustomerResponse getById(UUID customerId) {
        return CustomerResponse.from(queries.getById(customerId));
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_READ')")
    public PageResponse<CustomerResponse> search(CustomerStatus status, int page, int size) {
        return PageResponse.from(queries.search(status, new PageQuery(page, size)), CustomerResponse::from);
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Transactional
    public CustomerResponse update(UUID customerId, UpdateCustomerRequest request) {
        Customer customer = commands.update(new UpdateCustomerCommand(customerId, request.fullName(),
                request.email(), sanitizePhone(request.phone())));

        // Sincronizar con la tabla users si existe usuario para este cliente
        var existingUserOpt = userJpaRepository.findByCustomerId(customerId);
        if (existingUserOpt.isPresent()) {
            UserEntity user = existingUserOpt.get();
            user.setFullName(customer.getFullName());
            user.setEmail(customer.getEmail());
            if (request.password() != null && !request.password().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(request.password()));
                user.setPasswordChangedAt(Instant.now());
                user.setFailedLoginAttempts(0);
                user.setStatus("ACTIVE");
            }
            userJpaRepository.save(user);
        } else if (request.password() != null && !request.password().isBlank()) {
            // Si el cliente no tiene usuario asignado pero el admin le configuró una contraseña, creamos el usuario
            String baseUsername = customer.getEmail().split("@")[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
            if (baseUsername.length() < 4) {
                baseUsername = (baseUsername + "user").substring(0, Math.min(50, baseUsername.length() + 4));
            }
            if (baseUsername.length() > 40) {
                baseUsername = baseUsername.substring(0, 40);
            }
            if (userJpaRepository.findByUsernameWithRolesAndPermissions(baseUsername).isPresent()) {
                baseUsername = (baseUsername + "_" + customer.getDocumentNumber()).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
                if (baseUsername.length() > 50) {
                    baseUsername = baseUsername.substring(0, 50);
                }
            }

            Set<RoleEntity> roles = new HashSet<>();
            roleJpaRepository.findByCode("CUSTOMER").ifPresent(roles::add);

            Instant now = Instant.now();
            UserEntity newUser = UserEntity.builder()
                    .id(UUID.randomUUID())
                    .username(baseUsername)
                    .email(customer.getEmail().toLowerCase(Locale.ROOT))
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .fullName(customer.getFullName())
                    .customerId(customerId)
                    .status("ACTIVE")
                    .failedLoginAttempts(0)
                    .passwordChangedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .roles(roles)
                    .build();
            userJpaRepository.save(newUser);
        }

        return CustomerResponse.from(customer);
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CUSTOMER_DELETE')")
    public ResponseEntity<Void> deactivate(UUID customerId) {
        commands.deactivate(customerId);
        return ResponseEntity.noContent().build();
    }

    private static String sanitizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return phone.replaceAll("[\\s\\-\\(\\)]", "");
    }
}
