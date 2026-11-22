package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.escapa.backend.application.port.PasswordResetTokenRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.UserStatus;

class PasswordResetTokenRepositoryAdapterTest extends PostgresIntegrationTest {

    private static final long VALIDITY_HOURS = 2;

    @Autowired
    private PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
    }

    private void givenActiveUser() {
        final User user = new User(
                null, "Ana", UUID.randomUUID() + "@escapa.com", "hash",
                "STUDENT", null, UserStatus.ACTIVE, LocalDateTime.now());
        userId = userRepositoryPort.save(user).getId();
    }

    private static String sha256(String token) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void shouldSaveATokenAndFindItByTokenHash() {
        givenActiveUser();
        final String tokenHash = sha256("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now();

        final PasswordResetToken saved = passwordResetTokenRepositoryPort.save(
                PasswordResetToken.issue(userId, tokenHash, now, VALIDITY_HOURS));

        assertNotNull(saved.getId());
        final Optional<PasswordResetToken> found = passwordResetTokenRepositoryPort.findByTokenHash(tokenHash);
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(userId, found.get().getUserId());
        assertEquals(tokenHash, found.get().getTokenHash());
        assertNull(found.get().getUsedAt());
        assertFalse(found.get().isUsed());
        assertFalse(found.get().isExpired(now));
    }

    @Test
    void shouldUpdateAnExistingTokenWhenSavingAgain() {
        givenActiveUser();
        final String tokenHash = sha256("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now().withNano(0);
        final PasswordResetToken saved = passwordResetTokenRepositoryPort.save(
                PasswordResetToken.issue(userId, tokenHash, now, VALIDITY_HOURS));

        saved.markAsUsed(now.plusMinutes(5));
        passwordResetTokenRepositoryPort.save(saved);

        final PasswordResetToken reloaded = passwordResetTokenRepositoryPort.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(reloaded.isUsed());
        assertNotNull(reloaded.getUsedAt());
    }

    @Test
    void shouldMarkAnOpenTokenAsUsedOnlyOnce() {
        givenActiveUser();
        final String tokenHash = sha256("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now().withNano(0);
        final PasswordResetToken saved = passwordResetTokenRepositoryPort.save(
                PasswordResetToken.issue(userId, tokenHash, now, VALIDITY_HOURS));

        assertTrue(passwordResetTokenRepositoryPort.markAsUsed(saved.getId(), now.plusMinutes(1)));
        assertFalse(passwordResetTokenRepositoryPort.markAsUsed(saved.getId(), now.plusMinutes(2)));

        final PasswordResetToken reloaded = passwordResetTokenRepositoryPort.findByTokenHash(tokenHash).orElseThrow();
        assertEquals(now.plusMinutes(1), reloaded.getUsedAt());
    }

    @Test
    void shouldReturnEmptyForAnUnknownTokenHash() {
        assertTrue(passwordResetTokenRepositoryPort.findByTokenHash(sha256("inexistente")).isEmpty());
    }

    @Test
    void shouldInvalidateOpenTokensByExpiringThem() {
        givenActiveUser();
        final LocalDateTime now = LocalDateTime.now().withNano(0);
        final String openHash = sha256("open-" + UUID.randomUUID());
        final String usedHash = sha256("used-" + UUID.randomUUID());
        passwordResetTokenRepositoryPort.save(PasswordResetToken.issue(userId, openHash, now, VALIDITY_HOURS));
        final PasswordResetToken used = passwordResetTokenRepositoryPort.save(
                PasswordResetToken.issue(userId, usedHash, now, VALIDITY_HOURS));
        passwordResetTokenRepositoryPort.markAsUsed(used.getId(), now.plusMinutes(1));

        passwordResetTokenRepositoryPort.invalidateOpenTokens(userId, now.plusMinutes(10));

        final PasswordResetToken openReloaded =
                passwordResetTokenRepositoryPort.findByTokenHash(openHash).orElseThrow();
        assertTrue(openReloaded.isExpired(now.plusMinutes(10)));
        assertNull(openReloaded.getUsedAt());

        final PasswordResetToken usedReloaded =
                passwordResetTokenRepositoryPort.findByTokenHash(usedHash).orElseThrow();
        assertTrue(usedReloaded.isUsed());
    }

    @Test
    void shouldCascadeDeleteTokensWhenTheUserIsRemoved() {
        givenActiveUser();
        final String tokenHash = sha256("token-" + UUID.randomUUID());
        passwordResetTokenRepositoryPort.save(
                PasswordResetToken.issue(userId, tokenHash, LocalDateTime.now(), VALIDITY_HOURS));

        jdbcTemplate.update("delete from users where id = ?", userId);
        final UUID removed = userId;
        userId = null;

        assertTrue(passwordResetTokenRepositoryPort.findByTokenHash(tokenHash).isEmpty());
        assertTrue(userRepositoryPort.findById(removed).isEmpty());
    }
}
