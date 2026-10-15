package com.escapa.backend.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderAdapterTest {

    private static final String SECRET = "escapa-jwt-secret-key-development-minimum-256-bits-length";
    private static final long EXPIRATION_MINUTES = 60L;

    private JwtTokenProviderAdapter tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProviderAdapter(SECRET, EXPIRATION_MINUTES);
    }

    @Test
    void shouldGenerateValidTokenWithExpectedClaims() {
        final UUID userId = UUID.randomUUID();
        final String email = "usuario@escapa.com";
        final String role = "STUDENT";
        final String profile = "EMPLOYEE";

        final String token = tokenProvider.generateToken(userId, email, role, profile);

        assertNotNull(token);
        assertTrue(!token.isBlank());

        final Claims claims = Jwts.parser()
                .verifyWith(tokenProvider.getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(email, claims.get("email", String.class));
        assertEquals(role, claims.get("role", String.class));
        assertEquals(profile, claims.get("profile", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void shouldReturnCorrectExpirationSeconds() {
        assertEquals(3600L, tokenProvider.getExpirationSeconds());
    }
}

