package com.activitiesclub.activitiesclub_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.activitiesclub.activitiesclub_backend.ActivityVisibility;

public record ActivityUpsertRequest(
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

    Integer capacity,

    @DecimalMin(value = "0.00", inclusive = true)
    BigDecimal ticketPrice,

    ActivityVisibility visibility,

    Instant reservationOpensAt,

    Instant reservationClosesAt
) {}
