package com.activitiesclub.activitiesclub_backend.auth;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.activitiesclub.activitiesclub_backend.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {
    private static final String DEFAULT_SECRET = "dev-only-jwt-secret-change-me-dev-only-jwt-secret-change-me";
    private static final long DEFAULT_EXPIRATION_MS = 900_000L;

    private String secret;
    private long expMs;

    public JwtService(@Value("${app.jwt.secret:}") String secret, @Value("${app.jwt.expiration-ms:}") String expMs) {
        this.secret = secret == null || secret.isBlank() ? DEFAULT_SECRET : secret;
        this.expMs = expMs == null || expMs.isBlank() ? DEFAULT_EXPIRATION_MS : Long.parseLong(expMs);
    }

    public String generate(User user) {
        return Jwts.builder()
        .subject(user.getId().toString())
        .claim("role", user.getRole())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expMs))
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .compact();

    }
}
