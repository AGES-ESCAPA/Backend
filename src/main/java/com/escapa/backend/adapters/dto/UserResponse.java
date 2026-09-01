package com.escapa.backend.adapters.dto;

import java.time.LocalDateTime;

public record UserResponse(Integer id, String email, String userType, LocalDateTime createdAt) {
}
