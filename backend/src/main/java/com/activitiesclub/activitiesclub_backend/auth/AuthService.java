package com.activitiesclub.activitiesclub_backend.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.activitiesclub.activitiesclub_backend.ApprovalStatus;
import com.activitiesclub.activitiesclub_backend.User;
import com.activitiesclub.activitiesclub_backend.UserType;
import com.activitiesclub.activitiesclub_backend.UserRepository;
import com.activitiesclub.activitiesclub_backend.dto.AuthResponse;
import com.activitiesclub.activitiesclub_backend.dto.LoginRequest;
import com.activitiesclub.activitiesclub_backend.dto.RegisterRequest;
import com.activitiesclub.activitiesclub_backend.dto.RegistrationResponse;

@Service
public class AuthService {
    private static final String PENDING_APPROVAL_MESSAGE = "Your registration is awaiting admin approval.";
    private static final String DENIED_APPROVAL_MESSAGE = "Your registration request was denied. Please contact an admin before trying again.";
    private static final String REGISTRATION_SUBMITTED_MESSAGE = "Registration submitted for admin approval.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService) {
    this.users = users;
    this.encoder = encoder;
    this.jwtService = jwtService;
    }

    public RegistrationResponse register(RegisterRequest req) {
        String normalizedEmail = normalizeEmail(req.email());
        String normalizedUsername = normalizeUsername(req.username());
        String normalizedStudentNumber = normalizeField(req.studentNumber());
        String normalizedPhoneNumber = normalizeField(req.phoneNumber());
        User reusableDeniedUser = null;

        User existingUserByEmail = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existingUserByEmail != null) {
            if (existingUserByEmail.getApprovalStatus() != ApprovalStatus.DENIED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
            }

            reusableDeniedUser = existingUserByEmail;
        }
        boolean usernameInUse = reusableDeniedUser == null
            ? users.existsByUsernameIgnoreCase(normalizedUsername)
            : users.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, reusableDeniedUser.getId());
        if (usernameInUse) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already used");
        }

        String hash = encoder.encode(req.password());
        User user = reusableDeniedUser != null ? reusableDeniedUser : new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setUserType(req.userType());
        user.setStudentNumber(resolveStudentNumber(req.userType(), normalizedStudentNumber));
        user.setPhoneNumber(resolvePhoneNumber(req.userType(), normalizedPhoneNumber));
        user.setPasswordHash(hash);
        user.setAdmin(false);
        user.setApprovalStatus(ApprovalStatus.PENDING);
        users.save(user);
        return new RegistrationResponse(ApprovalStatus.PENDING, REGISTRATION_SUBMITTED_MESSAGE);
    }
    
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(normalizeEmail(req.email()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (user.getApprovalStatus() == ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, PENDING_APPROVAL_MESSAGE);
        }
        if (user.getApprovalStatus() == ApprovalStatus.DENIED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, DENIED_APPROVAL_MESSAGE);
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
