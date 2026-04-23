package com.activitiesclub.activitiesclub_backend;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {
    @Mock
    private UserRepository userRepository;

    @Test
    void rejectsDemotingTheLastRemainingAdmin() {
        User targetUser = new User();
        targetUser.setUsername("director");
        targetUser.setEmail("director@example.com");
        targetUser.setUserType(UserType.STAFF);
        targetUser.setAdmin(true);
        ReflectionTestUtils.setField(targetUser, "id", 2L);

        UserManagementService userManagementService = new UserManagementService(userRepository);

        when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
        when(userRepository.findAll()).thenReturn(List.of(targetUser));

        assertThatThrownBy(() ->
            userManagementService.updateAdminStatus(2L, false, 1L)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("At least one admin must remain");
    }
}
