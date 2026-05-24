package com.activitiesclub.activitiesclub_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportsClubSignupRepository extends JpaRepository<SportsClubSignup, Long> {
    Page<SportsClubSignup> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
