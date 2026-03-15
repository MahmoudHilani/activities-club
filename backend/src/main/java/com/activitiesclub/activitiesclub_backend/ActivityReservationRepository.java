package com.activitiesclub.activitiesclub_backend;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityReservationRepository extends JpaRepository<ActivityReservation, Long> {
    long countByActivityIdAndStatus(Long activityId, ReservationStatus status);

    long countByActivityIdAndStatusIn(Long activityId, Collection<ReservationStatus> statuses);

    boolean existsByActivityId(Long activityId);

    Optional<ActivityReservation> findByActivityIdAndUserId(Long activityId, Long userId);

    Optional<ActivityReservation> findFirstByActivityIdAndStatusOrderByReservedAtAscIdAsc(
        Long activityId,
        ReservationStatus status
    );
}
