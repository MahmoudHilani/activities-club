package com.activitiesclub.activitiesclub_backend.auth;

import java.security.Principal;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.activitiesclub.activitiesclub_backend.Role;
import com.activitiesclub.activitiesclub_backend.User;

public record AuthenticatedUser(Long id, String username, String email, Role role) implements Principal {
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public List<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getName() {
        return email;
    }
}
