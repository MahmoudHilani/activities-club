package com.activitiesclub.activitiesclub_backend.dto;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

import com.activitiesclub.activitiesclub_backend.Gender;
import com.activitiesclub.activitiesclub_backend.SportsClub;
import com.activitiesclub.activitiesclub_backend.SportsClubSignup;

public record SportsClubSignupResponse(
    Long id,
    String name,
    String email,
    String phoneNumber,
    String studentNumber,
    String course,
    Gender gender,
    Set<SportsClub> sportsClubs,
    Long userId,
    Instant createdAt
) {
    public static SportsClubSignupResponse from(SportsClubSignup signup) {
        return new SportsClubSignupResponse(
            signup.getId(),
            signup.getName(),
            signup.getEmail(),
            signup.getPhoneNumber(),
            signup.getStudentNumber(),
            signup.getCourse(),
            signup.getGender(),
            new TreeSet<>(signup.getSportsClubs()),
            signup.getUser() == null ? null : signup.getUser().getId(),
            signup.getCreatedAt()
        );
    }
}
