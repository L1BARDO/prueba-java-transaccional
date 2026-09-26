package com.switchtx.infrastructure.adapter.out.persistence.adapter;

import com.switchtx.application.port.out.UserRepositoryPort;
import com.switchtx.domain.model.user.Permission;
import com.switchtx.domain.model.user.Role;
import com.switchtx.domain.model.user.User;
import com.switchtx.domain.model.user.UserStatus;
import com.switchtx.infrastructure.adapter.out.persistence.entity.PermissionEntity;
import com.switchtx.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.switchtx.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.switchtx.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsernameWithRolesAndPermissions(username).map(this::toDomain);
    }

    @Override
    public void updateLastLogin(UUID userId, Instant loginAt) {
        repository.updateLastLogin(userId, loginAt);
    }

    @Override
    public void recordFailedLogin(String username, int maxAttempts) {
        repository.callRegisterFailedLogin(username, maxAttempts);
    }

    @Override
    public void resetFailedAttempts(UUID userId) {
        repository.resetFailedAttempts(userId);
    }

    private User toDomain(UserEntity entity) {
        Set<Role> roles = entity.getRoles().stream()
                .map(this::toRoleDomain)
                .collect(Collectors.toSet());

        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getCustomerId(),
                UserStatus.valueOf(entity.getStatus()),
                entity.getFailedLoginAttempts(),
                entity.getLastLoginAt(),
                entity.getPasswordChangedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                roles
        );
    }

    private Role toRoleDomain(RoleEntity entity) {
        Set<Permission> permissions = entity.getPermissions().stream()
                .map(p -> new Permission(p.getId(), p.getCode(), p.getModule(), p.getDescription()))
                .collect(Collectors.toSet());

        return new Role(entity.getId(), entity.getCode(), entity.getName(), entity.getDescription(),
                entity.isActive(), permissions);
    }
}
