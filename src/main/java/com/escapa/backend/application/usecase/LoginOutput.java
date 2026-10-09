package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.User;

public record LoginOutput(
        String accessToken,
        String tokenType,
        long expiresIn,
        User user,
        String profile
) {
}

