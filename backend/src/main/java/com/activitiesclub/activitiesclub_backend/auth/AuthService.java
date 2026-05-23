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
import com.activitiesclub.activitiesclub_backend.dto.SubmitRegistrationAppealRequest;
import io.jsonwebtoken.JwtException;

@Service
public class AuthService {
    private static final String PENDING_APPROVAL_MESSAGE = "Your registration is awaiting admin approval.";
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
        User existingUserByEmail = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existingUserByEmail != null) {
            if (existingUserByEmail.getApprovalStatus() == ApprovalStatus.DENIED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Denied registrations must be appealed from login");
            }

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
        }
        if (users.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already used");
        }

        String hash = encoder.encode(req.password());
        User user = new User();
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
            return new AuthResponse(null, jwtService.generateAppeal(user));
        }
        
        String token = jwtService.generate(user);
        return new AuthResponse(token, null);
    }

    public RegistrationResponse submitAppeal(String authorizationHeader, SubmitRegistrationAppealRequest req) {
        String token = bearerToken(authorizationHeader);
        Long userId;
        try {
            userId = jwtService.extractAppealUserId(token);
        } catch (JwtException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid appeal token");
        }

        User user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid appeal token"));
        if (user.getApprovalStatus() != ApprovalStatus.DENIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only denied registrations can be appealed");
        }

        String normalizedUsername = normalizeUsername(req.username());
        if (users.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already used");
        }

        user.setUsername(normalizedUsername);
        user.setUserType(req.userType());
        user.setStudentNumber(resolveStudentNumber(req.userType(), normalizeField(req.studentNumber())));
        user.setPhoneNumber(resolvePhoneNumber(req.userType(), normalizeField(req.phoneNumber())));
        if (req.userType() == UserType.STAFF) {
            user.setDateOfBirth(null);
        }
        user.setAdmin(false);
        user.setApprovalStatus(ApprovalStatus.PENDING);
        users.save(user);
        return new RegistrationResponse(ApprovalStatus.PENDING, REGISTRATION_SUBMITTED_MESSAGE);
    }

    private String bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Appeal token required");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Appeal token required");
        }

        return token;
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
