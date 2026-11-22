package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.escapa.backend.application.port.EmailChangeTokenRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.UserStatus;

class EmailChangeTokenRepositoryAdapterTest extends PostgresIntegrationTest {

    private static final long VALIDITY_HOURS = 72;

    @Autowired
    private EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID actorId;

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            jdbcTemplate.update("delete from users where id = ?", userId);
        }
        if (actorId != null) {
            jdbcTemplate.update("delete from users where id = ?", actorId);
        }
    }

    private void givenUsers() {
        actorId = userRepositoryPort.save(new User(
                null, "Admin", UUID.randomUUID() + "@escapa.com", "hash",
                "ADMIN", null, UserStatus.ACTIVE, LocalDateTime.now())).getId();
        userId = userRepositoryPort.save(new User(
                null, "Ana", UUID.randomUUID() + "@escapa.com", "hash",
                "STUDENT", null, UserStatus.ACTIVE, LocalDateTime.now())).getId();
    }

    @Test
    void shouldSaveATokenAndFindItByTokenHash() {
        givenUsers();
        final String tokenHash = EmailChangeToken.hashToken("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now();

        final EmailChangeToken saved = emailChangeTokenRepositoryPort.save(
                EmailChangeToken.issue(userId, "novo@escapa.com", tokenHash, actorId, now, VALIDITY_HOURS));

        assertNotNull(saved.getId());
        final Optional<EmailChangeToken> found = emailChangeTokenRepositoryPort.findByTokenHash(tokenHash);
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(userId, found.get().getUserId());
        assertEquals("novo@escapa.com", found.get().getNewEmail());
        assertEquals(actorId, found.get().getRequestedBy());
        assertFalse(found.get().isUsed());
    }

    @Test
    void shouldUpdateAnExistingTokenWhenSavingAgain() {
        givenUsers();
        final String tokenHash = EmailChangeToken.hashToken("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now().withNano(0);
        final EmailChangeToken saved = emailChangeTokenRepositoryPort.save(
                EmailChangeToken.issue(userId, "novo@escapa.com", tokenHash, actorId, now, VALIDITY_HOURS));

        saved.markAsUsed(now.plusMinutes(5));
        emailChangeTokenRepositoryPort.save(saved);

        final EmailChangeToken reloaded = emailChangeTokenRepositoryPort.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(reloaded.isUsed());
        assertNotNull(reloaded.getUsedAt());
    }

    @Test
    void shouldFindOpenTokenByUserId() {
        givenUsers();
        final LocalDateTime now = LocalDateTime.now().withNano(0);
        final String openHash = EmailChangeToken.hashToken("open-" + UUID.randomUUID());
        final String usedHash = EmailChangeToken.hashToken("used-" + UUID.randomUUID());
        emailChangeTokenRepositoryPort.save(
                EmailChangeToken.issue(userId, "aberto@escapa.com", openHash, actorId, now, VALIDITY_HOURS));
        final EmailChangeToken used = emailChangeTokenRepositoryPort.save(
                EmailChangeToken.issue(userId, "usado@escapa.com", usedHash, actorId, now, VALIDITY_HOURS));
        emailChangeTokenRepositoryPort.markAsUsed(used.getId(), now.plusMinutes(1));

        final EmailChangeToken open = emailChangeTokenRepositoryPort.findOpenByUserId(userId, now).orElseThrow();

        assertEquals(openHash, open.getTokenHash());
        assertEquals("aberto@escapa.com", open.getNewEmail());
        assertTrue(emailChangeTokenRepositoryPort.findOpenByUserId(UUID.randomUUID(), now).isEmpty());
    }

    @Test
    void shouldMarkAnOpenTokenAsUsedOnlyOnce() {
        givenUsers();
        final String tokenHash = EmailChangeToken.hashToken("token-" + UUID.randomUUID());
        final LocalDateTime now = LocalDateTime.now().withNano(0);
        final EmailChangeToken saved = emailChangeTokenRepositoryPort.save(
                EmailChangeToken.issue(userId, "novo@escapa.com", tokenHash, actorId, now, VALIDITY_HOURS));

        assertTrue(emailChangeTokenRepositoryPort.markAsUsed(saved.getId(), now.plusMinutes(1)));
        assertFalse(emailChangeTokenRepositoryPort.markAsUsed(saved.getId(), now.plusMinutes(2)));

        final EmailChangeToken reloaded = emailChangeTokenRepositoryPort.findByTokenHash(tokenHash).orElseThrow();
        assertEquals(now.plusMinutes(1), reloaded.getUsedAt());
    }

    @Test
    void shouldInvalidateOpenTokensByExpiringThem() {
        givenUsers();
        final LocalDateTime now = LocalDateTime.now().withNano(0);
        final String openHash = EmailChangeToken.hashToken("open-" + UUID.randomUUID());
        final String usedHash = EmailChangeToken.hashToken("used-" + UUID.randomUUID());
        emailChangeTokenRepositoryPort.save(
                EmailChangeToken.issue(userId, "aberto@escapa.com", openHash, actorId, now, VALIDITY_HOURS));
        final EmailChangeToken used = emailChangeTokenRepositoryPort.save(
                EmailChangeToken.issue(userId, "usado@escapa.com", usedHash, actorId, now, VALIDITY_HOURS));
        emailChangeTokenRepositoryPort.markAsUsed(used.getId(), now.plusMinutes(1));

        emailChangeTokenRepositoryPort.invalidateOpenTokens(userId, now.plusMinutes(10));

        final EmailChangeToken openReloaded =
                emailChangeTokenRepositoryPort.findByTokenHash(openHash).orElseThrow();
        assertTrue(openReloaded.isExpired(now.plusMinutes(10)));
        assertNull(openReloaded.getUsedAt());
        assertTrue(emailChangeTokenRepositoryPort.findOpenByUserId(userId, now.plusMinutes(10)).isEmpty());

        final EmailChangeToken usedReloaded =
                emailChangeTokenRepositoryPort.findByTokenHash(usedHash).orElseThrow();
        assertTrue(usedReloaded.isUsed());
    }

    @Test
    void shouldReturnEmptyForAnUnknownTokenHash() {
        assertTrue(emailChangeTokenRepositoryPort
                .findByTokenHash(EmailChangeToken.hashToken("inexistente")).isEmpty());
    }
}
