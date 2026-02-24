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
    private String secret;
    private long expMs;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expMs) {
        this.secret = secret;
        this.expMs = expMs;
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
