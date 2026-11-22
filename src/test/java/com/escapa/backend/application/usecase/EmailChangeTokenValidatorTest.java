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

import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.EmailChangeTokenExpiredException;
import com.escapa.backend.domain.user.EmailChangeTokenNotFoundException;

class EmailChangeTokenValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private InMemoryEmailChangeTokenRepositoryPort tokens;
    private EmailChangeTokenValidator validator;

    @BeforeEach
    void setUp() {
        tokens = new InMemoryEmailChangeTokenRepositoryPort();
        validator = new EmailChangeTokenValidator(tokens, CLOCK);
    }

    @Test
    void shouldReturnOpenToken() {
        final String raw = "token-aberto";
        final EmailChangeToken saved = tokens.save(EmailChangeToken.issue(
                UUID.randomUUID(),
                "novo@escapa.com",
                EmailChangeToken.hashToken(raw),
                UUID.randomUUID(),
                LocalDateTime.now(CLOCK),
                2));

        final EmailChangeToken found = validator.requireOpenToken(raw);

        assertEquals(saved.getId(), found.getId());
        assertEquals("novo@escapa.com", found.getNewEmail());
    }

    @Test
    void shouldRejectBlankOrUnknownToken() {
        assertThrows(EmailChangeTokenNotFoundException.class, () -> validator.requireOpenToken(null));
        assertThrows(EmailChangeTokenNotFoundException.class, () -> validator.requireOpenToken("  "));
        assertThrows(EmailChangeTokenNotFoundException.class, () -> validator.requireOpenToken("inexistente"));
    }

    @Test
    void shouldRejectUsedToken() {
        final String raw = "token-usado";
        final EmailChangeToken saved = tokens.save(EmailChangeToken.issue(
                UUID.randomUUID(),
                "novo@escapa.com",
                EmailChangeToken.hashToken(raw),
                UUID.randomUUID(),
                LocalDateTime.now(CLOCK),
                2));
        tokens.markAsUsed(saved.getId(), LocalDateTime.now(CLOCK));

        assertThrows(EmailChangeTokenNotFoundException.class, () -> validator.requireOpenToken(raw));
    }

    @Test
    void shouldRejectExpiredToken() {
        final String raw = "token-expirado";
        tokens.save(new EmailChangeToken(
                null,
                UUID.randomUUID(),
                "novo@escapa.com",
                EmailChangeToken.hashToken(raw),
                UUID.randomUUID(),
                LocalDateTime.now(CLOCK).minusMinutes(1),
                null,
                LocalDateTime.now(CLOCK).minusHours(3)));

        assertThrows(EmailChangeTokenExpiredException.class, () -> validator.requireOpenToken(raw));
    }
}
