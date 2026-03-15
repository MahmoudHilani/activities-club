package com.activitiesclub.activitiesclub_backend.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.activitiesclub.activitiesclub_backend.ActivityVisibility;

public record ActivityCreateRequest(
    @NotBlank
    @Size(max = 120)
    String title,

    @Size(max = 5000)
    String description,

    Instant startAt,

    Instant endAt,

    @Size(max = 160)
    String locationName,

    @Size(max = 255)
    String locationAddress,

    @Positive
    Integer capacity,

    ActivityVisibility visibility,

    Instant reservationOpensAt,

    Instant reservationClosesAt
) {}
