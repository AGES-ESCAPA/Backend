package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String userType,
        String status,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        final UserStatus status = user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE;
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUserType(),
                status.name(),
                user.getCreatedAt()
        );
    }
}
