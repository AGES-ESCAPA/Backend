package com.escapa.backend.user.dto;

import com.escapa.backend.user.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Saída pública de um usuário. Nunca expõe o hash da senha.
 * O campo se chama {@code userType} no JSON por contrato com o frontend, embora a coluna seja {@code role}.
 */
public record UserResponse(UUID id, String name, String email, String userType, LocalDateTime createdAt) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getCreatedAt());
    }
}
