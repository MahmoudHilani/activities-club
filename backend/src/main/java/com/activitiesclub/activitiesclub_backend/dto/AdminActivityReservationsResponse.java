package com.activitiesclub.activitiesclub_backend.dto;

import java.util.List;

public record AdminActivityReservationsResponse(
    ActivityResponse activity,
    List<AdminActivityReservationEntryResponse> reservations
) {}
