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
        final String id = user.getId() != null ? String.valueOf(user.getId()) : UUID.randomUUID().toString();
        final String name = user.getEmail() != null ? user.getEmail() : "";
        final String email = user.getEmail();
        final String role = user.getUserType() != null ? user.getUserType() : "USER";
        return new UserEntity(id, name, email, role);
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        Integer id = null;
        if (entity.getId() != null) {
            try {
                id = Integer.valueOf(entity.getId());
            } catch (final NumberFormatException ignored) {
                id = Math.abs(entity.getId().hashCode());
            }
        }
        return new User(
                id,
                entity.getEmail(),
                null,
                entity.getRole(),
                LocalDateTime.now(),
                new ArrayList<>()
        );
    }
}
