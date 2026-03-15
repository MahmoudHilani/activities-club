package com.activitiesclub.activitiesclub_backend;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.activitiesclub.activitiesclub_backend.auth.AuthenticatedUser;
import com.activitiesclub.activitiesclub_backend.dto.ReservationResponse;

@Service
public class ReservationService {
    private final ActivityReservationRepository reservationRepository;
    private final ActivityService activityService;
    private final UserRepository userRepository;

    public ReservationService(
        ActivityReservationRepository reservationRepository,
        ActivityService activityService,
        UserRepository userRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationResponse reserve(Long activityId, AuthenticatedUser currentUser) {
        assertReservableMember(currentUser);

        Activity activity = activityService.getReservableActivity(activityId);
        User user = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));

        ActivityReservation reservation = reservationRepository.findByActivityIdAndUserId(activityId, currentUser.id())
            .orElseGet(ActivityReservation::new);

        if (reservation.getId() != null
            && (reservation.getStatus() == ReservationStatus.RESERVED || reservation.getStatus() == ReservationStatus.WAITLISTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active reservation");
        }

        long confirmedCount = reservationRepository.countByActivityIdAndStatus(activityId, ReservationStatus.RESERVED);
        ReservationStatus nextStatus = hasCapacity(activity, confirmedCount)
            ? ReservationStatus.RESERVED
            : ReservationStatus.WAITLISTED;

        reservation.setActivity(activity);
        reservation.setUser(user);
        reservation.setStatus(nextStatus);
        reservation.setReservedAt(Instant.now());
        reservation.setCancelledAt(null);
        reservation.setNotes(null);

        reservationRepository.save(reservation);

        return buildResponse(activityId, nextStatus);
    }

    @Transactional
    public ReservationResponse cancel(Long activityId, AuthenticatedUser currentUser) {
        assertReservableMember(currentUser);

        ActivityReservation reservation = reservationRepository.findByActivityIdAndUserId(activityId, currentUser.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.RESERVED && reservation.getStatus() != ReservationStatus.WAITLISTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There is no active reservation to cancel");
        }

        boolean promoteWaitlist = reservation.getStatus() == ReservationStatus.RESERVED;

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(Instant.now());
        reservationRepository.save(reservation);

        if (promoteWaitlist) {
            reservationRepository.findFirstByActivityIdAndStatusOrderByReservedAtAscIdAsc(
                activityId,
                ReservationStatus.WAITLISTED
            ).ifPresent(waitlisted -> {
                waitlisted.setStatus(ReservationStatus.RESERVED);
                waitlisted.setCancelledAt(null);
                reservationRepository.save(waitlisted);
            });
        }

        return buildResponse(activityId, ReservationStatus.CANCELLED);
    }

    private ReservationResponse buildResponse(Long activityId, ReservationStatus status) {
        Activity activity = activityService.getActivity(activityId);
        long confirmedCount = reservationRepository.countByActivityIdAndStatus(activityId, ReservationStatus.RESERVED);
        long waitlistCount = reservationRepository.countByActivityIdAndStatus(activityId, ReservationStatus.WAITLISTED);
        Integer availableSpots = activity.getCapacity() == null ? null : Math.max(activity.getCapacity() - (int) confirmedCount, 0);
        boolean atCapacity = activity.getCapacity() != null && confirmedCount >= activity.getCapacity();

        return new ReservationResponse(
            activityId,
            status,
            confirmedCount,
            waitlistCount,
            availableSpots,
            atCapacity
        );
    }

    private boolean hasCapacity(Activity activity, long confirmedCount) {
        return activity.getCapacity() == null || confirmedCount < activity.getCapacity();
    }

    private void assertReservableMember(AuthenticatedUser currentUser) {
        if (currentUser.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot reserve activities");
        }
    }
}
