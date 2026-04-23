package com.activitiesclub.activitiesclub_backend;

import java.util.List;

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
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateAdminStatus(Long userId, boolean isAdmin, Long currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot change your own admin access");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isAdmin() == isAdmin) {
            return UserResponse.from(user);
        }

        if (!isAdmin && user.isAdmin() && remainingAdminCountExcluding(userId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At least one admin must remain");
        }

        user.setAdmin(isAdmin);
        return UserResponse.from(userRepository.save(user));
    }

    private long remainingAdminCountExcluding(Long excludedUserId) {
        return userRepository.findAll().stream()
            .filter(User::isAdmin)
            .filter(user -> !user.getId().equals(excludedUserId))
            .count();
    }
}
