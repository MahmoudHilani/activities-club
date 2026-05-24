package com.activitiesclub.activitiesclub_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.activitiesclub.activitiesclub_backend.auth.AuthenticatedUser;
import com.activitiesclub.activitiesclub_backend.dto.ActivityResponse;
import com.activitiesclub.activitiesclub_backend.dto.ReservationResponse;

import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityService activityService;
    private final ReservationService reservationService;

    public ActivityController(ActivityService activityService, ReservationService reservationService) {
        this.activityService = activityService;
        this.reservationService = reservationService;
    }

    @GetMapping("/public")
    public Page<ActivityResponse> listPublic(
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
        @RequestParam(required = false) @Size(max = 120) String q,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return activityService.listPublic(pageable, q, currentUser);
    }

    @GetMapping("/{activityId}")
    public ActivityResponse activity(
        @PathVariable Long activityId,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return activityService.getPublicActivity(activityId, currentUser);
    }

    @PostMapping("/{activityId}/reservations")
    public ReservationResponse reserve(
        @PathVariable Long activityId,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return reservationService.reserve(activityId, currentUser);
    }

    @DeleteMapping("/{activityId}/reservations/me")
    public ReservationResponse cancelReservation(
        @PathVariable Long activityId,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return reservationService.cancel(activityId, currentUser);
    }
}
