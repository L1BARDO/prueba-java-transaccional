package com.switchtx.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String fullName;
    private final UUID customerId;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String username, String email, String fullName, UUID customerId,
                         Set<String> roles, Set<String> permissions) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.customerId = customerId;
        this.roles = roles != null ? Set.copyOf(roles) : Collections.emptySet();
        this.permissions = permissions != null ? Set.copyOf(permissions) : Collections.emptySet();

        Set<GrantedAuthority> auths = new HashSet<>();
        for (String role : this.roles) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String perm : this.permissions) {
            auths.add(new SimpleGrantedAuthority(perm));
        }
        this.authorities = Collections.unmodifiableSet(auths);
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public UUID getCustomerId() { return customerId; }
    public Set<String> getRoles() { return roles; }
    public Set<String> getPermissions() { return permissions; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return null; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
