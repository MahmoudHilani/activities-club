package com.activitiesclub.activitiesclub_backend.auth;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activitiesclub.activitiesclub_backend.dto.AuthResponse;
import com.activitiesclub.activitiesclub_backend.dto.LoginRequest;
import com.activitiesclub.activitiesclub_backend.dto.RegisterRequest;
import com.activitiesclub.activitiesclub_backend.dto.RegistrationResponse;
import com.activitiesclub.activitiesclub_backend.dto.SubmitRegistrationAppealRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.accepted().body(authService.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PatchMapping("/appeal")
    public ResponseEntity<RegistrationResponse> submitAppeal(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
        @Valid @RequestBody SubmitRegistrationAppealRequest req
    ) {
        return ResponseEntity.accepted().body(authService.submitAppeal(authorizationHeader, req));
    }
}
