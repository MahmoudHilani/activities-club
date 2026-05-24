package com.activitiesclub.activitiesclub_backend.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.activitiesclub.activitiesclub_backend.Gender;
import com.activitiesclub.activitiesclub_backend.SportsClub;

public record SportsClubSignupRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @NotBlank
    @Email
    @Size(max = 120)
    String email,

    @NotBlank
    @Size(max = 30)
    String phoneNumber,

    @NotBlank
    @Size(max = 30)
    String studentNumber,

    @NotBlank
    @Size(max = 120)
    String course,

    @NotNull
    Gender gender,

    @NotEmpty
    Set<SportsClub> sportsClubs
) {}
