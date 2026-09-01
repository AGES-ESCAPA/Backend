package com.escapa.backend.adapters.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String userType, LocalDateTime createdAt) {
}
