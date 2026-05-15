package com.activitiesclub.activitiesclub_backend;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activitiesclub.activitiesclub_backend.auth.AuthenticatedUser;
import com.activitiesclub.activitiesclub_backend.dto.UpdateUserApprovalStatusRequest;
import com.activitiesclub.activitiesclub_backend.dto.UpdateUserAdminRequest;
import com.activitiesclub.activitiesclub_backend.dto.UpdateUserDateOfBirthRequest;
import com.activitiesclub.activitiesclub_backend.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userManagementService.listUsers();
    }

    @PatchMapping("/{userId}/approval")
    public UserResponse updateApprovalStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserApprovalStatusRequest request
    ) {
        return userManagementService.updateApprovalStatus(userId, request.approvalStatus(), request.dateOfBirth());
    }

    @PatchMapping("/{userId}/admin")
    public UserResponse updateAdminStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserAdminRequest request,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return userManagementService.updateAdminStatus(userId, request.isAdmin(), currentUser.id());
    }

    @PatchMapping("/{userId}/date-of-birth")
    public UserResponse updateDateOfBirth(
        @PathVariable Long userId,
        @RequestBody UpdateUserDateOfBirthRequest request
    ) {
        return userManagementService.updateDateOfBirth(userId, request.dateOfBirth());
    }
}
