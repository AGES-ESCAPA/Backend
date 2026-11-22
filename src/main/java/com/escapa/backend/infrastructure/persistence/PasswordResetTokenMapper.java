package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.infrastructure.persistence.entity.PasswordResetTokenEntity;

public final class PasswordResetTokenMapper {

    private PasswordResetTokenMapper() {
    }

    public static PasswordResetTokenEntity toEntity(PasswordResetToken token) {
        final UUID id = token.getId() != null ? token.getId() : UUID.randomUUID();
        final LocalDateTime createdAt = token.getCreatedAt() != null ? token.getCreatedAt() : LocalDateTime.now();
        return new PasswordResetTokenEntity(
                id, token.getUserId(), token.getTokenHash(), token.getExpiresAt(), token.getUsedAt(), createdAt);
    }

    public static void updateEntity(PasswordResetTokenEntity entity, PasswordResetToken token) {
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsedAt(token.getUsedAt());
    }

    public static PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt());
    }
}
