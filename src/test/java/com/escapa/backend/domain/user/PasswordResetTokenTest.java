package com.escapa.backend.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

    @Test
    void shouldHashTheTokenAsLowercaseSha256Hex() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                PasswordResetToken.hashToken("abc"));
    }

    @Test
    void shouldGenerateUrlSafeTokensThatAreNotEqual() {
        final String first = PasswordResetToken.generateToken();
        final String second = PasswordResetToken.generateToken();

        assertNotNull(first);
        assertNotEquals(first, second);
        assertFalse(first.contains("+"));
        assertFalse(first.contains("/"));
    }

    @Test
    void shouldIssueAnOpenTokenWithExpiry() {
        final UUID userId = UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.of(2026, 6, 15, 10, 0);
        final PasswordResetToken token = PasswordResetToken.issue(userId, "hash", now, 2);

        assertEquals(userId, token.getUserId());
        assertEquals("hash", token.getTokenHash());
        assertEquals(now.plusHours(2), token.getExpiresAt());
        assertFalse(token.isUsed());
        assertFalse(token.isExpired(now));
        assertTrue(token.isExpired(now.plusHours(2)));
    }

    @Test
    void shouldMarkAsUsed() {
        final PasswordResetToken token = PasswordResetToken.issue(
                UUID.randomUUID(), "hash", LocalDateTime.of(2026, 6, 15, 10, 0), 2);
        final LocalDateTime usedAt = LocalDateTime.of(2026, 6, 15, 11, 0);

        token.markAsUsed(usedAt);

        assertTrue(token.isUsed());
        assertEquals(usedAt, token.getUsedAt());
    }
}
