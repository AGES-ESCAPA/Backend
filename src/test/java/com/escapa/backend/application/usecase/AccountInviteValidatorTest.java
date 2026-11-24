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

import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.InviteExpiredException;
import com.escapa.backend.domain.user.InviteNotFoundException;

class AccountInviteValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private InMemoryAccountInviteRepositoryPort invites;
    private AccountInviteValidator validator;

    @BeforeEach
    void setUp() {
        invites = new InMemoryAccountInviteRepositoryPort();
        validator = new AccountInviteValidator(invites, CLOCK);
    }

    @Test
    void shouldReturnOpenInvite() {
        final String raw = "convite-aberto";
        final AccountInvite saved = invites.save(AccountInvite.issue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AccountInvite.hashToken(raw),
                LocalDateTime.now(CLOCK),
                72));

        final AccountInvite found = validator.requireOpenInvite(raw);

        assertEquals(saved.getId(), found.getId());
        assertEquals(saved.getUserId(), found.getUserId());
        assertEquals(saved.getCompanyId(), found.getCompanyId());
    }

    @Test
    void shouldRejectBlankOrUnknownInvite() {
        assertThrows(InviteNotFoundException.class, () -> validator.requireOpenInvite(null));
        assertThrows(InviteNotFoundException.class, () -> validator.requireOpenInvite("  "));
        assertThrows(InviteNotFoundException.class, () -> validator.requireOpenInvite("inexistente"));
    }

    @Test
    void shouldRejectUsedInviteEvenWhenAlsoExpired() {
        final String raw = "convite-usado";
        final AccountInvite saved = invites.save(new AccountInvite(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                AccountInvite.hashToken(raw),
                LocalDateTime.now(CLOCK).minusHours(1),
                LocalDateTime.now(CLOCK).minusMinutes(30),
                LocalDateTime.now(CLOCK).minusHours(3)));

        assertThrows(InviteNotFoundException.class, () -> validator.requireOpenInvite(raw));
        assertEquals(saved.getId(), invites.findByTokenHash(AccountInvite.hashToken(raw)).orElseThrow().getId());
    }

    @Test
    void shouldRejectExpiredOpenInvite() {
        final String raw = "convite-expirado";
        invites.save(new AccountInvite(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                AccountInvite.hashToken(raw),
                LocalDateTime.now(CLOCK).minusMinutes(1),
                null,
                LocalDateTime.now(CLOCK).minusHours(3)));

        assertThrows(InviteExpiredException.class, () -> validator.requireOpenInvite(raw));
    }
}
