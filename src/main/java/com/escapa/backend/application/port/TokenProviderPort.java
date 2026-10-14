package com.escapa.backend.application.port;

import java.util.UUID;

public interface TokenProviderPort {
    String generateToken(UUID userId, String email, String role, String profile);

    long getExpirationSeconds();
}

