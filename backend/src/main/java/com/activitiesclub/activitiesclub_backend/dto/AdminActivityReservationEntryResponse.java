package com.activitiesclub.activitiesclub_backend.dto;

import java.time.Instant;

import com.activitiesclub.activitiesclub_backend.ActivityReservation;
import com.activitiesclub.activitiesclub_backend.ReservationStatus;
import com.activitiesclub.activitiesclub_backend.User;

public record AdminActivityReservationEntryResponse(
    Long id,
    ReservationStatus status,
    Instant reservedAt,
    Instant cancelledAt,
    UserSummary user
) {
    public static AdminActivityReservationEntryResponse from(ActivityReservation reservation) {
        return new AdminActivityReservationEntryResponse(
            reservation.getId(),
            reservation.getStatus(),
            reservation.getReservedAt(),
            reservation.getCancelledAt(),
            UserSummary.from(reservation.getUser())
        );
    }

    public record UserSummary(Long id, String username, String email) {
        public static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getUsername(), user.getEmail());
        }
    }
}
