package com.switchtx.infrastructure.security;

import com.switchtx.application.port.in.auth.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LogManager.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationSeconds;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-seconds:86400}") long expirationSeconds,
            @Value("${security.jwt.issuer:switch-transaccional}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
        this.issuer = issuer;
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(user.username())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("userId", user.id().toString())
                .claim("email", user.email())
                .claim("fullName", user.fullName())
                .claim("customerId", user.customerId() != null ? user.customerId().toString() : null)
                .claim("roles", user.roles())
                .claim("permissions", user.permissions())
                .signWith(key)
                .compact();
    }

    public Optional<UserPrincipal> extractUserPrincipal(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            UUID userId = UUID.fromString(claims.get("userId", String.class));
            String email = claims.get("email", String.class);
            String fullName = claims.get("fullName", String.class);
            String customerIdStr = claims.get("customerId", String.class);
            UUID customerId = customerIdStr != null ? UUID.fromString(customerIdStr) : null;

            @SuppressWarnings("unchecked")
            List<String> rolesList = claims.get("roles", List.class);
            Set<String> roles = rolesList != null ? new HashSet<>(rolesList) : Collections.emptySet();

            @SuppressWarnings("unchecked")
            List<String> permissionsList = claims.get("permissions", List.class);
            Set<String> permissions = permissionsList != null ? new HashSet<>(permissionsList) : Collections.emptySet();

            return Optional.of(new UserPrincipal(userId, username, email, fullName, customerId, roles, permissions));
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token JWT inválido o expirado: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
