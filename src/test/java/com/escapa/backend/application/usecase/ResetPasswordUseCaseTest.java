package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.PasswordResetTokenExpiredException;
import com.escapa.backend.domain.user.PasswordResetTokenNotFoundException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class ResetPasswordUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 15, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final String TOKEN = "reset-token-valido";
    private static final String NEW_PASSWORD = "nova-senha-segura";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryPasswordResetTokenRepositoryPort tokens = new InMemoryPasswordResetTokenRepositoryPort();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final ResetPasswordUseCase useCase = new ResetPasswordUseCase(tokens, users, hasher, CLOCK);

    private User user;

    @BeforeEach
    void setUp() {
        user = users.save(new User(
                null, "Ana", "ana@escapa.com", "old-hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, NOW.minusDays(1)));
    }

    @Test
    void shouldUpdateThePasswordAndConsumeTheToken() {
        givenToken(TOKEN, NOW.plusHours(2), null);

        useCase.execute(TOKEN, NEW_PASSWORD);

        final User stored = users.findById(user.getId()).orElseThrow();
        assertTrue(hasher.matches(NEW_PASSWORD, stored.getPasswordHash()));
        assertTrue(tokens.findByTokenHash(PasswordResetToken.hashToken(TOKEN)).orElseThrow().isUsed());
    }

    @Test
    void shouldRejectAShortPassword() {
        givenToken(TOKEN, NOW.plusHours(2), null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(TOKEN, "curta"));
    }

    @Test
    void shouldRejectNullPassword() {
        givenToken(TOKEN, NOW.plusHours(2), null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(TOKEN, null));
    }

    @Test
    void shouldRejectUnknownToken() {
        assertThrows(PasswordResetTokenNotFoundException.class,
                () -> useCase.execute("inexistente", NEW_PASSWORD));
    }

    @Test
    void shouldRejectExpiredToken() {
        givenToken(TOKEN, NOW.minusMinutes(1), null);

        assertThrows(PasswordResetTokenExpiredException.class, () -> useCase.execute(TOKEN, NEW_PASSWORD));
    }

    @Test
    void shouldRejectWhenTheSameTokenIsUsedTwice() {
        givenToken(TOKEN, NOW.plusHours(2), null);
        useCase.execute(TOKEN, NEW_PASSWORD);

        assertThrows(PasswordResetTokenNotFoundException.class,
                () -> useCase.execute(TOKEN, "outra-senha-segura"));
    }

    @Test
    void shouldFailWhenTheUserNoLongerExists() {
        givenToken(TOKEN, NOW.plusHours(2), null);
        final UUID orphanUserId = UUID.randomUUID();
        tokens.save(new PasswordResetToken(
                null,
                orphanUserId,
                PasswordResetToken.hashToken("orphan-token"),
                NOW.plusHours(2),
                null,
                NOW));

        assertThrows(UserNotFoundException.class, () -> useCase.execute("orphan-token", NEW_PASSWORD));
    }

    @Test
    void shouldInvalidateOtherOpenTokensAfterReset() {
        givenToken(TOKEN, NOW.plusHours(2), null);
        givenToken("outro-token", NOW.plusHours(2), null);

        useCase.execute(TOKEN, NEW_PASSWORD);

        assertTrue(tokens.findByTokenHash(PasswordResetToken.hashToken("outro-token")).orElseThrow().isUsed());
    }

    private void givenToken(String rawToken, LocalDateTime expiresAt, LocalDateTime usedAt) {
        tokens.save(new PasswordResetToken(
                null,
                user.getId(),
                PasswordResetToken.hashToken(rawToken),
                expiresAt,
                usedAt,
                NOW.minusHours(1)));
    }
}
