package com.activitiesclub.activitiesclub_backend;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByStatusAndVisibility(ActivityStatus status, ActivityVisibility visibility, Pageable pageable);

    @Query("""
        select activity from Activity activity
        where activity.status = :status
          and activity.visibility = :visibility
          and (
            lower(activity.title) like concat('%', :query, '%') escape '\\'
            or lower(coalesce(activity.description, '')) like concat('%', :query, '%') escape '\\'
            or lower(coalesce(activity.locationName, '')) like concat('%', :query, '%') escape '\\'
            or lower(coalesce(activity.locationAddress, '')) like concat('%', :query, '%') escape '\\'
          )
        """)
    Page<Activity> findPublicMatchingSearch(
        @Param("status") ActivityStatus status,
        @Param("visibility") ActivityVisibility visibility,
        @Param("query") String query,
        Pageable pageable
    );

    Optional<Activity> findByIdAndStatusAndVisibility(Long id, ActivityStatus status, ActivityVisibility visibility);

    Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
