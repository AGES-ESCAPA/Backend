package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.PasswordResetTokenExpiredException;
import com.escapa.backend.domain.user.PasswordResetTokenNotFoundException;

class PasswordResetTokenValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private InMemoryPasswordResetTokenRepositoryPort tokens;
    private PasswordResetTokenValidator validator;

    @BeforeEach
    void setUp() {
        tokens = new InMemoryPasswordResetTokenRepositoryPort();
        validator = new PasswordResetTokenValidator(tokens, CLOCK);
    }

    @Test
    void shouldReturnOpenToken() {
        final String raw = "token-aberto";
        final PasswordResetToken saved = tokens.save(PasswordResetToken.issue(
                UUID.randomUUID(),
                PasswordResetToken.hashToken(raw),
                LocalDateTime.now(CLOCK),
                2));

        final PasswordResetToken found = validator.requireOpenToken(raw);

        assertEquals(saved.getId(), found.getId());
        assertEquals(saved.getUserId(), found.getUserId());
    }

    @Test
    void shouldRejectBlankOrUnknownToken() {
        assertThrows(PasswordResetTokenNotFoundException.class, () -> validator.requireOpenToken(null));
        assertThrows(PasswordResetTokenNotFoundException.class, () -> validator.requireOpenToken("  "));
        assertThrows(PasswordResetTokenNotFoundException.class, () -> validator.requireOpenToken("inexistente"));
    }

    @Test
    void shouldRejectUsedToken() {
        final String raw = "token-usado";
        final PasswordResetToken saved = tokens.save(PasswordResetToken.issue(
                UUID.randomUUID(),
                PasswordResetToken.hashToken(raw),
                LocalDateTime.now(CLOCK),
                2));
        tokens.markAsUsed(saved.getId(), LocalDateTime.now(CLOCK));

        assertThrows(PasswordResetTokenNotFoundException.class, () -> validator.requireOpenToken(raw));
    }

    @Test
    void shouldRejectExpiredToken() {
        final String raw = "token-expirado";
        tokens.save(new PasswordResetToken(
                null,
                UUID.randomUUID(),
                PasswordResetToken.hashToken(raw),
                LocalDateTime.now(CLOCK).minusMinutes(1),
                null,
                LocalDateTime.now(CLOCK).minusHours(3)));

        assertThrows(PasswordResetTokenExpiredException.class, () -> validator.requireOpenToken(raw));
    }
}
