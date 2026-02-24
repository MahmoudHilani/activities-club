package com.activitiesclub.activitiesclub_backend.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.activitiesclub.activitiesclub_backend.dto.AuthResponse;
import com.activitiesclub.activitiesclub_backend.dto.LoginRequest;
import com.activitiesclub.activitiesclub_backend.dto.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
