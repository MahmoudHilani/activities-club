package com.activitiesclub.activitiesclub_backend.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.activitiesclub.activitiesclub_backend.User;
import com.activitiesclub.activitiesclub_backend.UserType;
import com.activitiesclub.activitiesclub_backend.UserRepository;
import com.activitiesclub.activitiesclub_backend.dto.AuthResponse;
import com.activitiesclub.activitiesclub_backend.dto.LoginRequest;
import com.activitiesclub.activitiesclub_backend.dto.RegisterRequest;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
    this.users = users;
    this.encoder = encoder;
    this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        String normalizedEmail = normalizeEmail(req.email());
        String normalizedUsername = normalizeUsername(req.username());
        String normalizedStudentNumber = normalizeField(req.studentNumber());
        String normalizedPhoneNumber = normalizeField(req.phoneNumber());

        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
        }
        if (users.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already used");
        }

        String hash = encoder.encode(req.password());
        User u = new User();
        u.setUsername(normalizedUsername);
        u.setEmail(normalizedEmail);
        u.setUserType(req.userType());
        u.setStudentNumber(resolveStudentNumber(req.userType(), normalizedStudentNumber));
        u.setPhoneNumber(resolvePhoneNumber(req.userType(), normalizedPhoneNumber));
        u.setPasswordHash(hash);
        u.setAdmin(false);
        User saved = users.save(u);
        return new AuthResponse(jwtService.generate(saved));
    }
    
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(req.email()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        
        String token = jwtService.generate(user);
        return new AuthResponse(token);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username.trim();
    }

    private String normalizeField(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveStudentNumber(UserType userType, String studentNumber) {
        if (userType == UserType.STAFF) {
            return null;
        }
        if (studentNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student number is required for students");
        }

        return studentNumber;
    }

    private String resolvePhoneNumber(UserType userType, String phoneNumber) {
        if (userType == UserType.STAFF) {
            return null;
        }
        if (phoneNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required for students");
        }

        return phoneNumber;
    }
}
