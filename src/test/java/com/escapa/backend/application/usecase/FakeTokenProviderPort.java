package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.TokenProviderPort;

import java.util.UUID;

final class FakeTokenProviderPort implements TokenProviderPort {

    static final String DEFAULT_TOKEN = "mocked-jwt-token";
    static final long DEFAULT_EXPIRATION_SECONDS = 3600L;

    private String lastGeneratedToken = DEFAULT_TOKEN;
    private UUID lastUserId;
    private String lastRole;
    private String lastProfile;

    @Override
    public String generateToken(UUID userId, String email, String role, String profile) {
        this.lastUserId = userId;
        this.lastRole = role;
        this.lastProfile = profile;
        this.lastGeneratedToken = "mocked-jwt-" + profile.toLowerCase();
        return this.lastGeneratedToken;
    }

    @Override
    public long getExpirationSeconds() {
        return DEFAULT_EXPIRATION_SECONDS;
    }

    UUID getLastUserId() {
        return lastUserId;
    }

    String getLastRole() {
        return lastRole;
    }

    String getLastProfile() {
        return lastProfile;
    }
}

