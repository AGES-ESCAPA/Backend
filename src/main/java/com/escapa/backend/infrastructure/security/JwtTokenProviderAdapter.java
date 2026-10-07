package com.escapa.backend.infrastructure.security;

import com.escapa.backend.application.port.TokenProviderPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private static final long SECONDS_PER_MINUTE = 60L;

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtTokenProviderAdapter(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public String generateToken(UUID userId, String email, String role, String profile) {
        final Instant now = Instant.now();
        final Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .claim("profile", profile)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    @Override
    public long getExpirationSeconds() {
        return expirationMinutes * SECONDS_PER_MINUTE;
    }

    public SecretKey getKey() {
        return key;
    }
}

