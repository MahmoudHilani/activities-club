package com.activitiesclub.activitiesclub_backend;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityReservationRepository extends JpaRepository<ActivityReservation, Long> {
    long countByActivityIdAndStatus(Long activityId, ReservationStatus status);

    long countByActivityIdAndStatusIn(Long activityId, Collection<ReservationStatus> statuses);

    boolean existsByActivityId(Long activityId);

    Optional<ActivityReservation> findByActivityIdAndUserId(Long activityId, Long userId);

    Optional<ActivityReservation> findFirstByActivityIdAndStatusOrderByReservedAtAscIdAsc(
        Long activityId,
        ReservationStatus status
    );

    @Query("""
        select reservation
        from ActivityReservation reservation
        join fetch reservation.user
        where reservation.activity.id = :activityId
        """)
    List<ActivityReservation> findAllByActivityIdWithUser(@Param("activityId") Long activityId);
}
