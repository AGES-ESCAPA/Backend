package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.entity.User;

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
        return new UserEntity(id, user.getName(), user.getEmail(), user.getPasswordHash(),
                user.getUserType(), createdAt);
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
                entity.getCreatedAt(),
                new ArrayList<>()
        );
    }
}
