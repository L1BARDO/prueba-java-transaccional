package com.switchtx.infrastructure.adapter.out.persistence.repository;

import com.switchtx.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    @Query("""
            SELECT DISTINCT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions p
            WHERE u.username = :username
            """)
    Optional<UserEntity> findByUsernameWithRolesAndPermissions(@Param("username") String username);

    @Modifying
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :loginAt WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") UUID userId, @Param("loginAt") Instant loginAt);

    @Modifying
    @Query("UPDATE UserEntity u SET u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void resetFailedAttempts(@Param("userId") UUID userId);

    @Query("SELECT u FROM UserEntity u WHERE u.customerId = :customerId")
    Optional<UserEntity> findByCustomerId(@Param("customerId") UUID customerId);

    @Modifying
    @Query(value = "INSERT INTO user_roles (user_id, role_id) SELECT :userId, r.id FROM roles r WHERE r.code = 'CUSTOMER' ON CONFLICT DO NOTHING", nativeQuery = true)
    void assignCustomerRole(@Param("userId") UUID userId);

    @Query(value = "SELECT fn_register_failed_login(:username, :maxAttempts)", nativeQuery = true)
    String callRegisterFailedLogin(@Param("username") String username, @Param("maxAttempts") int maxAttempts);
}
