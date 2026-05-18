package com.anushabazaar.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessExpiryHours;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiry-hours}") long accessExpiryHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiryHours = accessExpiryHours;
    }

    public String generateAccessToken(String email, String userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("userId", userId, "role", role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessExpiryHours, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
