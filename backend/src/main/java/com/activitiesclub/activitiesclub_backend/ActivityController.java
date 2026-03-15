package com.activitiesclub.activitiesclub_backend;

import java.time.Instant;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.activitiesclub.activitiesclub_backend.auth.AuthenticatedUser;
import com.activitiesclub.activitiesclub_backend.dto.ActivityCreateRequest;
import com.activitiesclub.activitiesclub_backend.dto.ActivityResponse;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    public ActivityController(ActivityRepository activityRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/public")
    public Page<ActivityResponse> listPublic(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return activityRepository
            .findByStatusAndVisibility(ActivityStatus.PUBLISHED, ActivityVisibility.PUBLIC, pageable)
            .map(ActivityResponse::from);
    }

    @GetMapping
    public Page<ActivityResponse> list(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return activityRepository.findAll(pageable).map(ActivityResponse::from);
    }

    @PostMapping
    public ActivityResponse create(
        @Valid @RequestBody ActivityCreateRequest body,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        validateRequest(body);

        User organizer = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));

        Activity activity = new Activity();
        activity.setTitle(body.title().trim());
        activity.setDescription(trimToNull(body.description()));
        activity.setOrganizer(organizer);
        activity.setStartAt(body.startAt());
        activity.setEndAt(body.endAt());
        activity.setLocationName(trimToNull(body.locationName()));
        activity.setLocationAddress(trimToNull(body.locationAddress()));
        activity.setCapacity(body.capacity());
        activity.setVisibility(body.visibility() == null ? ActivityVisibility.PUBLIC : body.visibility());
        activity.setReservationOpensAt(body.reservationOpensAt());
        activity.setReservationClosesAt(body.reservationClosesAt());

        return ActivityResponse.from(activityRepository.save(activity));
    }

    @PatchMapping("/{activityId}/publish")
    public ActivityResponse publish(
        @PathVariable Long activityId,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        Activity activity = activityRepository.findById(activityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        if (!canManageActivity(activity, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot publish this activity");
        }
        if (activity.getStatus() == ActivityStatus.CANCELLED || activity.getStatus() == ActivityStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft activities can be published");
        }

        activity.setStatus(ActivityStatus.PUBLISHED);
        return ActivityResponse.from(activityRepository.save(activity));
    }

    private void validateRequest(ActivityCreateRequest body) {
        validateAfter("endAt", body.endAt(), "startAt", body.startAt(), false);
        validateAfter("reservationClosesAt", body.reservationClosesAt(), "reservationOpensAt", body.reservationOpensAt(), true);
    }

    private boolean canManageActivity(Activity activity, AuthenticatedUser currentUser) {
        if (currentUser.role() == Role.ADMIN) {
            return true;
        }

        User organizer = activity.getOrganizer();
        return organizer != null && organizer.getId().equals(currentUser.id());
    }

    private void validateAfter(String currentName, Instant current, String previousName, Instant previous, boolean equalAllowed) {
        if (current == null || previous == null) {
            return;
        }

        boolean valid = equalAllowed ? !current.isBefore(previous) : current.isAfter(previous);
        if (!valid) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                currentName + " must be " + (equalAllowed ? "on or after " : "after ") + previousName
            );
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
