package com.activitiesclub.activitiesclub_backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.activitiesclub.activitiesclub_backend.Role;
import com.activitiesclub.activitiesclub_backend.User;
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

    public AuthResponse register( RegisterRequest req) {
        if(users.existsByEmail(req.email())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");;
        String hash = encoder.encode(req.password());
        User u = new User();
        u.setUsername(req.username());
        u.setEmail(req.email());
        u.setPasswordHash(hash);
        u.setRole(Role.STUDENT);
        User saved = users.save(u);
        return new AuthResponse(jwtService.generate(saved));
    }
    
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if(!encoder.matches(req.password(), user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        
        String token = jwtService.generate(user);
        return new AuthResponse(token);
    }

}
