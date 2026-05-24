package com.activitiesclub.activitiesclub_backend;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.activitiesclub.activitiesclub_backend.auth.AuthenticatedUser;
import com.activitiesclub.activitiesclub_backend.dto.AdminActivityReservationEntryResponse;
import com.activitiesclub.activitiesclub_backend.dto.AdminActivityReservationsResponse;
import com.activitiesclub.activitiesclub_backend.dto.ActivityResponse;
import com.activitiesclub.activitiesclub_backend.dto.ActivityUpsertRequest;

@Service
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final ActivityReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;

    public ActivityService(
        ActivityRepository activityRepository,
        ActivityReservationRepository reservationRepository,
        UserRepository userRepository,
        ImageStorageService imageStorageService
    ) {
        this.activityRepository = activityRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> listPublic(Pageable pageable, String search, AuthenticatedUser currentUser) {
        Long currentUserId = currentUser == null ? null : currentUser.id();
        String normalizedSearch = normalizeSearch(search);
        Page<Activity> activities = normalizedSearch == null
            ? activityRepository.findByStatusAndVisibility(ActivityStatus.PUBLISHED, ActivityVisibility.PUBLIC, pageable)
            : activityRepository.findPublicMatchingSearch(
                ActivityStatus.PUBLISHED,
                ActivityVisibility.PUBLIC,
                normalizedSearch,
                pageable
            );

        return activities
            .map(activity -> toResponse(activity, currentUserId));
    }

    @Transactional(readOnly = true)
    public ActivityResponse getPublicActivity(Long activityId, AuthenticatedUser currentUser) {
        Long currentUserId = currentUser == null ? null : currentUser.id();

        Activity activity = activityRepository
            .findByIdAndStatusAndVisibility(activityId, ActivityStatus.PUBLISHED, ActivityVisibility.PUBLIC)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));

        return toResponse(activity, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> listAdmin(Pageable pageable) {
        return activityRepository.findAllByOrderByCreatedAtDesc(pageable).map(activity -> toResponse(activity, null));
    }

    @Transactional(readOnly = true)
    public ActivityResponse getAdminActivity(Long activityId) {
        return toResponse(getActivityById(activityId), null);
    }

    @Transactional(readOnly = true)
    public AdminActivityReservationsResponse getAdminReservations(Long activityId) {
        Activity activity = getActivityById(activityId);
        List<AdminActivityReservationEntryResponse> reservations = reservationRepository.findAllByActivityIdWithUser(activityId)
            .stream()
            .sorted(adminReservationComparator())
            .map(AdminActivityReservationEntryResponse::from)
            .toList();

        return new AdminActivityReservationsResponse(toResponse(activity, null), reservations);
    }

    @Transactional
    public ActivityResponse create(ActivityUpsertRequest request, org.springframework.web.multipart.MultipartFile image, Long userId) {
        validateRequest(request);

        User organizer = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));

        Activity activity = new Activity();
        applyUpsert(activity, request);
        activity.setOrganizer(organizer);
        activity.setImagePath(imageStorageService.storeRequiredImage(image));

        return toResponse(activityRepository.save(activity), null);
    }

    @Transactional
    public ActivityResponse update(
        Long activityId,
        ActivityUpsertRequest request,
        org.springframework.web.multipart.MultipartFile image
    ) {
        validateRequest(request);

        Activity activity = getActivity(activityId);
        applyUpsert(activity, request);
        activity.setImagePath(imageStorageService.replaceImage(activity.getImagePath(), image));

        return toResponse(activityRepository.save(activity), null);
    }

    @Transactional
    public ActivityResponse publish(Long activityId) {
        Activity activity = getActivity(activityId);

        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.CANCELLED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only draft or cancelled activities can be published"
            );
        }

        activity.setStatus(ActivityStatus.PUBLISHED);
        return toResponse(activityRepository.save(activity), null);
    }

    @Transactional
    public ActivityResponse cancel(Long activityId) {
        Activity activity = getActivity(activityId);

        if (activity.getStatus() == ActivityStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed activities cannot be cancelled");
        }
        if (activity.getStatus() == ActivityStatus.CANCELLED) {
            return toResponse(activity, null);
        }

        activity.setStatus(ActivityStatus.CANCELLED);
        return toResponse(activityRepository.save(activity), null);
    }

    @Transactional
    public void delete(Long activityId) {
        Activity activity = getActivity(activityId);

        if (activity.getStatus() != ActivityStatus.DRAFT && activity.getStatus() != ActivityStatus.CANCELLED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Only draft or cancelled activities without reservation history can be deleted"
            );
        }
        if (reservationRepository.existsByActivityId(activityId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Activities with reservation history cannot be deleted"
            );
        }

        imageStorageService.deleteManagedImage(activity.getImagePath());
        activityRepository.delete(activity);
    }

    @Transactional(readOnly = true)
    public Activity getReservableActivity(Long activityId) {
        Activity activity = getActivityById(activityId);

        if (activity.getStatus() != ActivityStatus.PUBLISHED || activity.getVisibility() != ActivityVisibility.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This activity is not open for reservations");
        }

        Instant now = Instant.now();
        if (activity.getReservationOpensAt() != null && now.isBefore(activity.getReservationOpensAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservations are not open yet");
        }
        if (activity.getReservationClosesAt() != null && now.isAfter(activity.getReservationClosesAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reservations are closed for this activity");
        }

        return activity;
    }

    @Transactional(readOnly = true)
    public Activity getActivity(Long activityId) {
        return getActivityById(activityId);
    }

    @Transactional(readOnly = true)
    public ActivityResponse toResponse(Activity activity, Long currentUserId) {
        long confirmedReservationCount = reservationRepository.countByActivityIdAndStatus(
            activity.getId(),
            ReservationStatus.RESERVED
        );
        long waitlistCount = reservationRepository.countByActivityIdAndStatus(
            activity.getId(),
            ReservationStatus.WAITLISTED
        );

        ReservationStatus currentUserReservationStatus = null;
        if (currentUserId != null) {
            currentUserReservationStatus = reservationRepository.findByActivityIdAndUserId(activity.getId(), currentUserId)
                .map(ActivityReservation::getStatus)
                .filter(EnumSet.of(ReservationStatus.RESERVED, ReservationStatus.WAITLISTED)::contains)
                .orElse(null);
        }

        return ActivityResponse.from(activity, confirmedReservationCount, waitlistCount, currentUserReservationStatus);
    }

    private Activity getActivityById(Long activityId) {
        return activityRepository.findById(activityId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
    }

    private void applyUpsert(Activity activity, ActivityUpsertRequest request) {
        activity.setTitle(request.title().trim());
        activity.setDescription(trimToNull(request.description()));
        activity.setStartAt(request.startAt());
        activity.setEndAt(request.endAt());
        activity.setLocationName(trimToNull(request.locationName()));
        activity.setLocationAddress(trimToNull(request.locationAddress()));
        activity.setCapacity(request.capacity());
        activity.setTicketPrice(request.ticketPrice() == null ? java.math.BigDecimal.ZERO : request.ticketPrice());
        activity.setOvernight(request.isOvernight());
        activity.setVisibility(request.visibility() == null ? ActivityVisibility.PUBLIC : request.visibility());
        activity.setReservationOpensAt(request.reservationOpensAt());
        activity.setReservationClosesAt(request.reservationClosesAt());
    }

    private void validateRequest(ActivityUpsertRequest request) {
        if (request.capacity() != null && request.capacity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "capacity must be greater than 0");
        }
        if (request.ticketPrice() != null && request.ticketPrice().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketPrice must be zero or greater");
        }
        validateAfter("endAt", request.endAt(), "startAt", request.startAt(), false);
        validateAfter(
            "reservationClosesAt",
            request.reservationClosesAt(),
            "reservationOpensAt",
            request.reservationOpensAt(),
            true
        );
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

    private String normalizeSearch(String search) {
        String normalized = trimToNull(search);
        if (normalized == null) {
            return null;
        }

        return normalized
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    private Comparator<ActivityReservation> adminReservationComparator() {
        return (left, right) -> {
            int statusComparison = Integer.compare(statusRank(left.getStatus()), statusRank(right.getStatus()));
            if (statusComparison != 0) {
                return statusComparison;
            }

            if (left.getStatus() == ReservationStatus.CANCELLED && right.getStatus() == ReservationStatus.CANCELLED) {
                Comparator<Instant> cancelledAtComparator = Comparator.nullsLast(Comparator.reverseOrder());
                int cancelledComparison = cancelledAtComparator.compare(left.getCancelledAt(), right.getCancelledAt());
                if (cancelledComparison != 0) {
                    return cancelledComparison;
                }
            }

            Comparator<Instant> reservedAtComparator = Comparator.nullsLast(Comparator.naturalOrder());
            int reservedAtComparison = reservedAtComparator.compare(left.getReservedAt(), right.getReservedAt());
            if (reservedAtComparison != 0) {
                return reservedAtComparison;
            }

            return Comparator.nullsLast(Comparator.<Long>naturalOrder()).compare(left.getId(), right.getId());
        };
    }

    private int statusRank(ReservationStatus status) {
        return switch (status) {
            case RESERVED -> 0;
            case WAITLISTED -> 1;
            case CANCELLED -> 2;
            case ATTENDED -> 3;
            case NO_SHOW -> 4;
        };
    }
}
