package com.activitiesclub.activitiesclub_backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.activitiesclub.activitiesclub_backend.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private static final long DEFAULT_EXPIRATION_MS = 900_000L;
    private static final String PURPOSE_CLAIM = "purpose";
    private static final String MEMBER_PURPOSE = "member";
    private static final String APPEAL_PURPOSE = "appeal";

    private final SecretKey signingKey;
    private final long expMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms:}") String expMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be configured");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
        }

        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expMs = expMs == null || expMs.isBlank() ? DEFAULT_EXPIRATION_MS : Long.parseLong(expMs);
    }

    public String generate(User user) {
        return generate(user, MEMBER_PURPOSE);
    }

    public String generateAppeal(User user) {
        return generate(user, APPEAL_PURPOSE);
    }

    private String generate(User user, String purpose) {
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim(PURPOSE_CLAIM, purpose)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expMs))
            .signWith(signingKey)
            .compact();
    }

    public Long extractUserId(String token) {
        return extractUserId(token, MEMBER_PURPOSE);
    }

    public Long extractAppealUserId(String token) {
        return extractUserId(token, APPEAL_PURPOSE);
    }

    private Long extractUserId(String token, String requiredPurpose) {
        Claims claims = parseClaims(token);
        if (!requiredPurpose.equals(claims.get(PURPOSE_CLAIM, String.class))) {
            throw new JwtException("Invalid token purpose");
        }
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new JwtException("Invalid token subject", ex);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
