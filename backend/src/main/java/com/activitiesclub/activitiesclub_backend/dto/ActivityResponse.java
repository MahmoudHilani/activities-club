package com.activitiesclub.activitiesclub_backend.dto;

import java.time.Instant;

import com.activitiesclub.activitiesclub_backend.Activity;
import com.activitiesclub.activitiesclub_backend.ActivityStatus;
import com.activitiesclub.activitiesclub_backend.ActivityVisibility;
import com.activitiesclub.activitiesclub_backend.User;

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
    ActivityStatus status,
    ActivityVisibility visibility,
    Instant reservationOpensAt,
    Instant reservationClosesAt,
    Instant createdAt,
    Instant updatedAt
) {
    public static ActivityResponse from(Activity activity) {
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
            activity.getStatus(),
            activity.getVisibility(),
            activity.getReservationOpensAt(),
            activity.getReservationClosesAt(),
            activity.getCreatedAt(),
            activity.getUpdatedAt()
        );
    }

    public record OrganizerResponse(Long id, String username) {
        public static OrganizerResponse from(User user) {
            return user == null ? null : new OrganizerResponse(user.getId(), user.getUsername());
        }
    }
}
