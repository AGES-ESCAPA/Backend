package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.PasswordResetTokenDetails;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.PasswordResetTokenExpiredException;
import com.escapa.backend.domain.user.PasswordResetTokenNotFoundException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class GetPasswordResetTokenUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 15, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final String TOKEN = "reset-token-valido";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryPasswordResetTokenRepositoryPort tokens = new InMemoryPasswordResetTokenRepositoryPort();
    private final GetPasswordResetTokenUseCase useCase =
            new GetPasswordResetTokenUseCase(tokens, users, CLOCK);

    private User user;

    @BeforeEach
    void setUp() {
        user = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, NOW.minusDays(1)));
    }

    @Test
    void shouldReturnTheEmailForAnOpenToken() {
        givenToken(TOKEN, NOW.plusHours(2), null);

        final PasswordResetTokenDetails details = useCase.execute(TOKEN);

        assertEquals("ana@escapa.com", details.email());
    }

    @Test
    void shouldRejectUnknownToken() {
        assertThrows(PasswordResetTokenNotFoundException.class, () -> useCase.execute("inexistente"));
    }

    @Test
    void shouldRejectUsedToken() {
        givenToken(TOKEN, NOW.plusHours(2), NOW.minusMinutes(5));

        assertThrows(PasswordResetTokenNotFoundException.class, () -> useCase.execute(TOKEN));
    }

    @Test
    void shouldRejectExpiredToken() {
        givenToken(TOKEN, NOW.minusMinutes(1), null);

        assertThrows(PasswordResetTokenExpiredException.class, () -> useCase.execute(TOKEN));
    }

    @Test
    void shouldRejectBlankToken() {
        assertThrows(PasswordResetTokenNotFoundException.class, () -> useCase.execute("  "));
        assertThrows(PasswordResetTokenNotFoundException.class, () -> useCase.execute(null));
    }

    @Test
    void shouldFailWhenTheUserNoLongerExists() {
        final UUID orphanUserId = UUID.randomUUID();
        tokens.save(new PasswordResetToken(
                null,
                orphanUserId,
                PasswordResetToken.hashToken("orphan-token"),
                NOW.plusHours(2),
                null,
                NOW));

        assertThrows(UserNotFoundException.class, () -> useCase.execute("orphan-token"));
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
