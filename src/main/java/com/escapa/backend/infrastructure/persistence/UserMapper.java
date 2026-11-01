package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        final UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        final LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();
        return new UserEntity(
                id,
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getUserType(),
                toEntityStatus(user.getStatus()),
                user.getAvatarUrl(),
                createdAt
        );
    }

    public static void updateEntity(UserEntity entity, User user) {
        entity.setName(user.getName());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRole(user.getUserType());
        entity.setStatus(toEntityStatus(user.getStatus()));
        entity.setProfileAvatarUrl(user.getAvatarUrl());
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getProfileAvatarUrl(),
                toDomainStatus(entity.getStatus()),
                entity.getCreatedAt()
        );
    }

    private static com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus toEntityStatus(
            UserStatus status) {
        return com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.valueOf(status.name());
    }

    private static UserStatus toDomainStatus(
            com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus status) {
        return UserStatus.valueOf(status.name());
    }
}
