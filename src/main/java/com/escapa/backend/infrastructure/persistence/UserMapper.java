package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.user.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public static User toDomain(UserEntity entity) {
        return new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getRole());
    }
}
