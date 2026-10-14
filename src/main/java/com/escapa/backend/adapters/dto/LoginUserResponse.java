package com.escapa.backend.adapters.dto;

import java.util.UUID;

public record LoginUserResponse(
        UUID id,
        String name,
        String email,
        String profile
) {
}

