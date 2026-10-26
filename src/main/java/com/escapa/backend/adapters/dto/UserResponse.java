package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String userType, LocalDateTime createdAt) {


    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getUserType(),
                user.getCreatedAt());
    }
}
