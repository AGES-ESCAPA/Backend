package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        final UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        final LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
        final com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus entityStatus =
                user.getStatus() != null && user.getStatus() == UserStatus.INACTIVE
                        ? com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.INACTIVE
                        : com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.ACTIVE;

        return new UserEntity(
                id,
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getUserType(),
                entityStatus,
                createdAt
        );
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        final UserStatus domainStatus = entity.getStatus() != null
                && "INACTIVE".equalsIgnoreCase(entity.getStatus().name())
                ? UserStatus.INACTIVE
                : UserStatus.ACTIVE;

        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                domainStatus,
                entity.getCreatedAt(),
                new ArrayList<>()
        );
    }
}
