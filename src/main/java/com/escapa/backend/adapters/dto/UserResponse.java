package com.escapa.backend.adapters.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String userType,
        String status,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return from(user, user.getUserType());
    }

    /** {@code userType} já resolvido (ex.: STUDENT vinculado à empresa → EMPLOYEE). */
    public static UserResponse from(User user, String userType) {
        final UserStatus status = user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE;
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                userType,
                status.name(),
                user.getCreatedAt()
        );
    }
}
