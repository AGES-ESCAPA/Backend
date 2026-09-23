package com.escapa.backend.adapters.dto;

public record LoginResponse(
        String accessToken,
        String tokenType
) {}
