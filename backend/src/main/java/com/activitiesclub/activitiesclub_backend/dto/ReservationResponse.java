package com.activitiesclub.activitiesclub_backend.dto;

import com.activitiesclub.activitiesclub_backend.ReservationStatus;

public record ReservationResponse(
    Long activityId,
    ReservationStatus status,
    long confirmedReservationCount,
    long waitlistCount,
    Integer availableSpots,
    boolean atCapacity
) {}
