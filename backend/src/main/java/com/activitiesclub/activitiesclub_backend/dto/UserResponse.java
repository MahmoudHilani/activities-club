package com.activitiesclub.activitiesclub_backend.dto;

import java.time.Instant;

import com.activitiesclub.activitiesclub_backend.ApprovalStatus;
import com.activitiesclub.activitiesclub_backend.User;
import com.activitiesclub.activitiesclub_backend.UserType;

public record UserResponse(
    Long id,
    String username,
    String email,
    String studentNumber,
    String phoneNumber,
    UserType userType,
    ApprovalStatus approvalStatus,
    boolean isAdmin,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getStudentNumber(),
            user.getPhoneNumber(),
            user.getUserType(),
            user.getApprovalStatus(),
            user.isAdmin(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
