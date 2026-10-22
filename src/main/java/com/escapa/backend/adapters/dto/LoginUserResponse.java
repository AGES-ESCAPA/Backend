package com.escapa.backend.adapters.dto;

import java.util.UUID;

import com.escapa.backend.domain.entity.User;

public record LoginUserResponse(
        UUID id,
        String name,
        String email,
        String profile
) {

    public static LoginUserResponse from(User user, String profile) {
        return new LoginUserResponse(user.getId(), user.getName(), user.getEmail(), profile);
    }
}

