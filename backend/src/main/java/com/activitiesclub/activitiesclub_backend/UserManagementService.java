package com.activitiesclub.activitiesclub_backend;

import java.util.List;
import java.util.Comparator;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.activitiesclub.activitiesclub_backend.dto.UserResponse;

@Service
public class UserManagementService {
    private final UserRepository userRepository;

    public UserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
            .sorted(Comparator
                .comparing((User user) -> user.getApprovalStatus() != ApprovalStatus.PENDING)
                .thenComparing(User::getCreatedAt, Comparator.reverseOrder()))
            .map(UserResponse::from)
            .toList();
    }

    @Transactional
    public UserResponse updateAdminStatus(Long userId, boolean isAdmin, Long currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot change your own admin access");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only approved users can receive admin access");
        }

        if (user.isAdmin() == isAdmin) {
            return UserResponse.from(user);
        }

        if (!isAdmin && user.isAdmin() && remainingAdminCountExcluding(userId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one admin must remain");
        }

        user.setAdmin(isAdmin);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateApprovalStatus(Long userId, ApprovalStatus approvalStatus, LocalDate dateOfBirth) {
        if (approvalStatus == ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval status must be APPROVED or DENIED");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending users can be approved or denied");
        }

        if (approvalStatus == ApprovalStatus.APPROVED && user.getUserType() == UserType.STUDENT) {
            validateDateOfBirth(dateOfBirth);
            user.setDateOfBirth(dateOfBirth);
        }

        user.setApprovalStatus(approvalStatus);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateDateOfBirth(Long userId, LocalDate dateOfBirth) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getUserType() != UserType.STUDENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Date of birth can only be recorded for students");
        }

        validateDateOfBirth(dateOfBirth);
        user.setDateOfBirth(dateOfBirth);
        return UserResponse.from(userRepository.save(user));
    }

    private void validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateOfBirth cannot be in the future");
        }
    }

    private long remainingAdminCountExcluding(Long excludedUserId) {
        return userRepository.findAll().stream()
            .filter(User::isAdmin)
            .filter(user -> !user.getId().equals(excludedUserId))
            .count();
    }
}
