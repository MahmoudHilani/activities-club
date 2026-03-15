package com.activitiesclub.activitiesclub_backend.dto;

import java.time.Instant;

import com.activitiesclub.activitiesclub_backend.Role;
import com.activitiesclub.activitiesclub_backend.User;

public record UserResponse(
    Long id,
    String username,
    String email,
    Role role,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
