package com.activitiesclub.activitiesclub_backend.dto;

import com.activitiesclub.activitiesclub_backend.UserType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitRegistrationAppealRequest(
    @NotBlank
    @Size(max = 30)
    String username,

    @NotNull
    UserType userType,

    @Size(max = 30)
    String studentNumber,

    @Size(max = 30)
    String phoneNumber
) {}
