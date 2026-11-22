package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.infrastructure.persistence.entity.EmailChangeTokenEntity;

public final class EmailChangeTokenMapper {

    private EmailChangeTokenMapper() {
    }

    public static EmailChangeTokenEntity toEntity(EmailChangeToken token) {
        final UUID id = token.getId() != null ? token.getId() : UUID.randomUUID();
        final LocalDateTime createdAt = token.getCreatedAt() != null ? token.getCreatedAt() : LocalDateTime.now();
        return new EmailChangeTokenEntity(
                id,
                token.getUserId(),
                token.getNewEmail(),
                token.getTokenHash(),
                token.getRequestedBy(),
                token.getExpiresAt(),
                token.getUsedAt(),
                createdAt);
    }

    public static void updateEntity(EmailChangeTokenEntity entity, EmailChangeToken token) {
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsedAt(token.getUsedAt());
    }

    public static EmailChangeToken toDomain(EmailChangeTokenEntity entity) {
        return new EmailChangeToken(
                entity.getId(),
                entity.getUserId(),
                entity.getNewEmail(),
                entity.getTokenHash(),
                entity.getRequestedBy(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt());
    }
}
