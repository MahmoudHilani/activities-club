package com.activitiesclub.activitiesclub_backend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activitiesclub.activitiesclub_backend.dto.SportsClubSignupResponse;

@RestController
@RequestMapping("/api/admin/sports-club-signups")
public class AdminSportsClubSignupController {
    private final SportsClubSignupService signupService;

    public AdminSportsClubSignupController(SportsClubSignupService signupService) {
        this.signupService = signupService;
    }

    @GetMapping
    public Page<SportsClubSignupResponse> list(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return signupService.listAdmin(pageable);
    }
}
