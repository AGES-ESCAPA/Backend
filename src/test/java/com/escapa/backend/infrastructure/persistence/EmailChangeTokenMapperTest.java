package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.infrastructure.persistence.entity.EmailChangeTokenEntity;

class EmailChangeTokenMapperTest {

    @Test
    void shouldMapDomainToEntityGeneratingIdAndCreatedAtWhenMissing() {
        final UUID userId = UUID.randomUUID();
        final UUID actorId = UUID.randomUUID();
        final LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        final EmailChangeToken token = new EmailChangeToken(
                null, userId, "novo@escapa.com", "hash", actorId, expiresAt, null, null);

        final EmailChangeTokenEntity entity = EmailChangeTokenMapper.toEntity(token);

        assertNotNull(entity.getId());
        assertEquals(userId, entity.getUserId());
        assertEquals("novo@escapa.com", entity.getNewEmail());
        assertEquals("hash", entity.getTokenHash());
        assertEquals(actorId, entity.getRequestedBy());
        assertEquals(expiresAt, entity.getExpiresAt());
        assertNull(entity.getUsedAt());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void shouldPreserveIdAndCreatedAtWhenPresent() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final EmailChangeToken token = new EmailChangeToken(
                id,
                UUID.randomUUID(),
                "novo@escapa.com",
                "hash",
                UUID.randomUUID(),
                createdAt.plusHours(2),
                null,
                createdAt);

        final EmailChangeTokenEntity entity = EmailChangeTokenMapper.toEntity(token);

        assertEquals(id, entity.getId());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void shouldUpdateOnlyExpiresAtAndUsedAt() {
        final EmailChangeTokenEntity entity = new EmailChangeTokenEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "novo@escapa.com",
                "hash",
                UUID.randomUUID(),
                LocalDateTime.of(2026, 6, 15, 12, 0),
                null,
                LocalDateTime.of(2026, 6, 15, 10, 0));
        final LocalDateTime newExpires = LocalDateTime.of(2026, 6, 15, 11, 0);
        final LocalDateTime usedAt = LocalDateTime.of(2026, 6, 15, 10, 30);
        final EmailChangeToken token = new EmailChangeToken(
                entity.getId(),
                entity.getUserId(),
                "outro@escapa.com",
                "other-hash",
                entity.getRequestedBy(),
                newExpires,
                usedAt,
                entity.getCreatedAt());

        EmailChangeTokenMapper.updateEntity(entity, token);

        assertEquals(newExpires, entity.getExpiresAt());
        assertEquals(usedAt, entity.getUsedAt());
        assertEquals("hash", entity.getTokenHash());
        assertEquals("novo@escapa.com", entity.getNewEmail());
    }

    @Test
    void shouldMapEntityToDomain() {
        final UUID id = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID actorId = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final LocalDateTime expiresAt = createdAt.plusHours(2);
        final LocalDateTime usedAt = createdAt.plusMinutes(30);
        final EmailChangeTokenEntity entity = new EmailChangeTokenEntity(
                id, userId, "novo@escapa.com", "hash", actorId, expiresAt, usedAt, createdAt);

        final EmailChangeToken domain = EmailChangeTokenMapper.toDomain(entity);

        assertEquals(id, domain.getId());
        assertEquals(userId, domain.getUserId());
        assertEquals("novo@escapa.com", domain.getNewEmail());
        assertEquals("hash", domain.getTokenHash());
        assertEquals(actorId, domain.getRequestedBy());
        assertEquals(expiresAt, domain.getExpiresAt());
        assertEquals(usedAt, domain.getUsedAt());
        assertEquals(createdAt, domain.getCreatedAt());
    }
}
