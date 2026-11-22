package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.infrastructure.persistence.entity.PasswordResetTokenEntity;

class PasswordResetTokenMapperTest {

    @Test
    void shouldMapDomainToEntityGeneratingIdAndCreatedAtWhenMissing() {
        final UUID userId = UUID.randomUUID();
        final LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        final PasswordResetToken token = new PasswordResetToken(
                null, userId, "hash", expiresAt, null, null);

        final PasswordResetTokenEntity entity = PasswordResetTokenMapper.toEntity(token);

        assertNotNull(entity.getId());
        assertEquals(userId, entity.getUserId());
        assertEquals("hash", entity.getTokenHash());
        assertEquals(expiresAt, entity.getExpiresAt());
        assertNull(entity.getUsedAt());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void shouldPreserveIdAndCreatedAtWhenPresent() {
        final UUID id = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final PasswordResetToken token = new PasswordResetToken(
                id, userId, "hash", createdAt.plusHours(2), null, createdAt);

        final PasswordResetTokenEntity entity = PasswordResetTokenMapper.toEntity(token);

        assertEquals(id, entity.getId());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void shouldUpdateOnlyExpiresAtAndUsedAt() {
        final PasswordResetTokenEntity entity = new PasswordResetTokenEntity(
                UUID.randomUUID(), UUID.randomUUID(), "hash",
                LocalDateTime.of(2026, 6, 15, 12, 0), null, LocalDateTime.of(2026, 6, 15, 10, 0));
        final LocalDateTime newExpires = LocalDateTime.of(2026, 6, 15, 11, 0);
        final LocalDateTime usedAt = LocalDateTime.of(2026, 6, 15, 10, 30);
        final PasswordResetToken token = new PasswordResetToken(
                entity.getId(), entity.getUserId(), "other-hash", newExpires, usedAt, entity.getCreatedAt());

        PasswordResetTokenMapper.updateEntity(entity, token);

        assertEquals(newExpires, entity.getExpiresAt());
        assertEquals(usedAt, entity.getUsedAt());
        assertEquals("hash", entity.getTokenHash());
    }

    @Test
    void shouldMapEntityToDomain() {
        final UUID id = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final LocalDateTime expiresAt = createdAt.plusHours(2);
        final LocalDateTime usedAt = createdAt.plusMinutes(30);
        final PasswordResetTokenEntity entity = new PasswordResetTokenEntity(
                id, userId, "hash", expiresAt, usedAt, createdAt);

        final PasswordResetToken domain = PasswordResetTokenMapper.toDomain(entity);

        assertEquals(id, domain.getId());
        assertEquals(userId, domain.getUserId());
        assertEquals("hash", domain.getTokenHash());
        assertEquals(expiresAt, domain.getExpiresAt());
        assertEquals(usedAt, domain.getUsedAt());
        assertEquals(createdAt, domain.getCreatedAt());
    }
}
