package com.activitiesclub.activitiesclub_backend.auth;

import java.security.Principal;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.activitiesclub.activitiesclub_backend.User;

public record AuthenticatedUser(Long id, String username, String email, boolean isAdmin) implements Principal {
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getEmail(), user.isAdmin());
    }

    public List<? extends GrantedAuthority> authorities() {
        if (!isAdmin) {
            return List.of();
        }

        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getName() {
        return email;
    }
}
