package com.activitiesclub.activitiesclub_backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.activitiesclub.activitiesclub_backend.Activity;
import com.activitiesclub.activitiesclub_backend.ActivityStatus;
import com.activitiesclub.activitiesclub_backend.ActivityVisibility;
import com.activitiesclub.activitiesclub_backend.ReservationStatus;
import com.activitiesclub.activitiesclub_backend.User;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public record ActivityResponse(
    Long id,
    String title,
    String description,
    OrganizerResponse organizer,
    Instant startAt,
    Instant endAt,
    String locationName,
    String locationAddress,
    Integer capacity,
    String imageUrl,
    BigDecimal ticketPrice,
    ActivityStatus status,
    ActivityVisibility visibility,
    Instant reservationOpensAt,
    Instant reservationClosesAt,
    long confirmedReservationCount,
    long waitlistCount,
    Integer availableSpots,
    boolean atCapacity,
    ReservationStatus currentUserReservationStatus,
    Instant createdAt,
    Instant updatedAt
) {
    public static ActivityResponse from(
        Activity activity,
        long confirmedReservationCount,
        long waitlistCount,
        ReservationStatus currentUserReservationStatus
    ) {
        Integer availableSpots = null;
        boolean atCapacity = false;

        if (activity.getCapacity() != null) {
            availableSpots = Math.max(activity.getCapacity() - (int) confirmedReservationCount, 0);
            atCapacity = confirmedReservationCount >= activity.getCapacity();
        }

        return new ActivityResponse(
            activity.getId(),
            activity.getTitle(),
            activity.getDescription(),
            OrganizerResponse.from(activity.getOrganizer()),
            activity.getStartAt(),
            activity.getEndAt(),
            activity.getLocationName(),
            activity.getLocationAddress(),
            activity.getCapacity(),
            buildImageUrl(activity.getImagePath()),
            activity.getTicketPrice(),
            activity.getStatus(),
            activity.getVisibility(),
            activity.getReservationOpensAt(),
            activity.getReservationClosesAt(),
            confirmedReservationCount,
            waitlistCount,
            availableSpots,
            atCapacity,
            currentUserReservationStatus,
            activity.getCreatedAt(),
            activity.getUpdatedAt()
        );
    }

    private static String buildImageUrl(String imagePath) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/")
            .path(imagePath)
            .toUriString();
    }

    public record OrganizerResponse(Long id, String username) {
        public static OrganizerResponse from(User user) {
            return user == null ? null : new OrganizerResponse(user.getId(), user.getUsername());
        }
    }
}
