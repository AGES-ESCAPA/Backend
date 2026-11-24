package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.infrastructure.persistence.entity.AccountInviteEntity;

class AccountInviteMapperTest {

    @Test
    void shouldMapDomainToEntityGeneratingIdAndCreatedAtWhenMissing() {
        final UUID userId = UUID.randomUUID();
        final UUID companyId = UUID.randomUUID();
        final LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        final AccountInvite invite = new AccountInvite(
                null, userId, companyId, "hash", expiresAt, null, null);

        final AccountInviteEntity entity = AccountInviteMapper.toEntity(invite);

        assertNotNull(entity.getId());
        assertEquals(userId, entity.getUserId());
        assertEquals(companyId, entity.getCompanyId());
        assertEquals("hash", entity.getTokenHash());
        assertEquals(expiresAt, entity.getExpiresAt());
        assertNull(entity.getUsedAt());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void shouldPreserveIdAndCreatedAtWhenPresent() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final AccountInvite invite = new AccountInvite(
                id, UUID.randomUUID(), UUID.randomUUID(), "hash",
                createdAt.plusHours(72), null, createdAt);

        final AccountInviteEntity entity = AccountInviteMapper.toEntity(invite);

        assertEquals(id, entity.getId());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void shouldUpdateOnlyExpiresAtAndUsedAt() {
        final AccountInviteEntity entity = new AccountInviteEntity(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "hash",
                LocalDateTime.of(2026, 6, 15, 12, 0), null, LocalDateTime.of(2026, 6, 15, 10, 0));
        final LocalDateTime newExpires = LocalDateTime.of(2026, 6, 15, 11, 0);
        final LocalDateTime usedAt = LocalDateTime.of(2026, 6, 15, 10, 30);
        final AccountInvite invite = new AccountInvite(
                entity.getId(), entity.getUserId(), entity.getCompanyId(), "other-hash",
                newExpires, usedAt, entity.getCreatedAt());

        AccountInviteMapper.updateEntity(entity, invite);

        assertEquals(newExpires, entity.getExpiresAt());
        assertEquals(usedAt, entity.getUsedAt());
        assertEquals("hash", entity.getTokenHash());
    }

    @Test
    void shouldMapEntityToDomain() {
        final UUID id = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID companyId = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final LocalDateTime expiresAt = createdAt.plusHours(72);
        final LocalDateTime usedAt = createdAt.plusMinutes(30);
        final AccountInviteEntity entity = new AccountInviteEntity(
                id, userId, companyId, "hash", expiresAt, usedAt, createdAt);

        final AccountInvite domain = AccountInviteMapper.toDomain(entity);

        assertEquals(id, domain.getId());
        assertEquals(userId, domain.getUserId());
        assertEquals(companyId, domain.getCompanyId());
        assertEquals("hash", domain.getTokenHash());
        assertEquals(expiresAt, domain.getExpiresAt());
        assertEquals(usedAt, domain.getUsedAt());
        assertEquals(createdAt, domain.getCreatedAt());
    }
}
