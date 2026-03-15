package com.activitiesclub.activitiesclub_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByStatusAndVisibility(ActivityStatus status, ActivityVisibility visibility, Pageable pageable);
}
