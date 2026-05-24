package com.activitiesclub.activitiesclub_backend;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.activitiesclub.activitiesclub_backend.auth.AuthenticatedUser;
import com.activitiesclub.activitiesclub_backend.dto.SportsClubSignupRequest;
import com.activitiesclub.activitiesclub_backend.dto.SportsClubSignupResponse;

@RestController
@RequestMapping("/api/sports-club-signups")
public class SportsClubSignupController {
    private final SportsClubSignupService signupService;

    public SportsClubSignupController(SportsClubSignupService signupService) {
        this.signupService = signupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SportsClubSignupResponse submit(
        @Valid @RequestBody SportsClubSignupRequest request,
        @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return signupService.submit(request, currentUser);
    }
}
