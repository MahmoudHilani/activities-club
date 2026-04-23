package com.activitiesclub.activitiesclub_backend.dto;

import com.activitiesclub.activitiesclub_backend.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(max = 30)
    String username,

    @NotBlank
    @Email
    @Size(max = 120)
    String email,

    @NotNull
    UserType userType,

    @Size(max = 30)
    String studentNumber,

    @Size(max = 30)
    String phoneNumber,

    @NotBlank
    @Size(min = 8, max = 72)
    String password
) {}
