package com.activitiesclub.activitiesclub_backend;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.activitiesclub.activitiesclub_backend.auth.AuthenticatedUser;
import com.activitiesclub.activitiesclub_backend.dto.AdminActivityReservationsResponse;
import com.activitiesclub.activitiesclub_backend.dto.ActivityResponse;
import com.activitiesclub.activitiesclub_backend.dto.ActivityUpsertRequest;

@RestController
@RequestMapping("/api/admin/activities")
public class AdminActivityController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ActivityService activityService;
    private final Validator validator;

    public AdminActivityController(ActivityService activityService, Validator validator) {
        this.activityService = activityService;
        this.validator = validator;
    }

    @GetMapping
    public Page<ActivityResponse> list(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return activityService.listAdmin(pageable);
    }

    @GetMapping("/{activityId}/reservations")
    public AdminActivityReservationsResponse reservations(@PathVariable Long activityId) {
        return activityService.getAdminReservations(activityId);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ActivityResponse create(
        @RequestPart("activity") MultipartFile activityPayload,
        @RequestPart("image") MultipartFile image,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return activityService.create(parseActivityPayload(activityPayload), image, currentUser.id());
    }

    @PutMapping(path = "/{activityId}", consumes = "multipart/form-data")
    public ActivityResponse update(
        @PathVariable Long activityId,
        @RequestPart("activity") MultipartFile activityPayload,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return activityService.update(activityId, parseActivityPayload(activityPayload), image);
    }

    @PatchMapping("/{activityId}/publish")
    public ActivityResponse publish(@PathVariable Long activityId) {
        return activityService.publish(activityId);
    }

    @PatchMapping("/{activityId}/cancel")
    public ActivityResponse cancel(@PathVariable Long activityId) {
        return activityService.cancel(activityId);
    }

    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long activityId) {
        activityService.delete(activityId);
    }

    private ActivityUpsertRequest parseActivityPayload(MultipartFile activityPayload) {
        ActivityUpsertRequest request;
        try {
            JsonNode root = OBJECT_MAPPER.readTree(new String(activityPayload.getBytes(), StandardCharsets.UTF_8));
            request = new ActivityUpsertRequest(
                text(root, "title"),
                nullableText(root, "description"),
                parseInstant(root, "startAt"),
                parseInstant(root, "endAt"),
                nullableText(root, "locationName"),
                nullableText(root, "locationAddress"),
                root.path("capacity").isNull() || root.path("capacity").isMissingNode() ? null : root.path("capacity").asInt(),
                root.path("ticketPrice").isNull() || root.path("ticketPrice").isMissingNode()
                    ? null
                    : root.path("ticketPrice").decimalValue(),
                root.path("visibility").isNull() || root.path("visibility").isMissingNode()
                    ? null
                    : ActivityVisibility.valueOf(root.path("visibility").asText()),
                parseInstant(root, "reservationOpensAt"),
                parseInstant(root, "reservationClosesAt")
            );
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid activity payload: " + exception.getMessage()
            );
        }

        Set<ConstraintViolation<ActivityUpsertRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, violations.iterator().next().getMessage());
        }

        return request;
    }

    private String text(JsonNode root, String fieldName) {
        return root.path(fieldName).asText(null);
    }

    private String nullableText(JsonNode root, String fieldName) {
        return root.path(fieldName).isNull() || root.path(fieldName).isMissingNode()
            ? null
            : root.path(fieldName).asText(null);
    }

    private Instant parseInstant(JsonNode root, String fieldName) {
        String value = nullableText(root, fieldName);
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
