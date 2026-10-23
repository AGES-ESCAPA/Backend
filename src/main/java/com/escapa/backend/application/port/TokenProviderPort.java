package com.escapa.backend.application.port;

import java.util.UUID;

import com.escapa.backend.application.model.TokenClaims;

public interface TokenProviderPort {
    String generateToken(UUID userId, String email, String role, String profile);

    TokenClaims validateToken(String token);

    long getExpirationSeconds();
}

