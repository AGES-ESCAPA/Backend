package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Avatar upload result")
public record AvatarResponse(String avatarUrl) {
}