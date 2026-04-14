package com.activitiesclub.activitiesclub_backend;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByStatusAndVisibility(ActivityStatus status, ActivityVisibility visibility, Pageable pageable);

    Optional<Activity> findByIdAndStatusAndVisibility(Long id, ActivityStatus status, ActivityVisibility visibility);

    Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
