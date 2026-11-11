package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.InviteExpiredException;
import com.escapa.backend.domain.user.InviteNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateInvitePasswordUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final String TOKEN = "convite-joao-dev";
    private static final String PASSWORD = "senha-segura";
    private static final String PENDING_HASH = "PENDING-NO-PASSWORD-SET";
    private static final UUID COMPANY_ID = UUID.randomUUID();

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryAccountInviteRepositoryPort invites = new InMemoryAccountInviteRepositoryPort();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final FakeTokenProviderPort tokenProvider = new FakeTokenProviderPort();
    private final CreateInvitePasswordUseCase useCase =
            new CreateInvitePasswordUseCase(invites, users, hasher, tokenProvider, CLOCK);

    private User employee;

    @BeforeEach
    void setUp() {
        employee = users.save(new User(null, "João da Silva", "joao.silva@empresa.com.br",
                PENDING_HASH, "STUDENT", null, UserStatus.PENDING, NOW.minusDays(1)));
    }

    @Test
    void shouldActivateTheAccountInvalidateTheInviteAndReturnTheAccessToken() {
        givenInvite(NOW.plusHours(72), null);

        final LoginOutput output = useCase.execute(TOKEN, PASSWORD);

        final User stored = users.findById(employee.getId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, stored.getStatus());
        assertTrue(hasher.matches(PASSWORD, stored.getPasswordHash()));
        assertEquals(NOW, storedInvite().getUsedAt());
        assertEquals("mocked-jwt-employee", output.accessToken());
        assertEquals("Bearer", output.tokenType());
        assertEquals("EMPLOYEE", output.profile());
        assertEquals(employee.getId(), output.user().getId());
        assertEquals(employee.getId(), tokenProvider.getLastUserId());
        assertEquals("STUDENT", tokenProvider.getLastRole());
    }

    @Test
    void shouldLoginWithTheCreatedPassword() {
        givenInvite(NOW.plusHours(72), null);
        users.linkUserToCompany(employee.getId());
        useCase.execute(TOKEN, PASSWORD);

        final LoginOutput login = new LoginUseCase(users, hasher, tokenProvider)
                .execute("joao.silva@empresa.com.br", PASSWORD);

        assertEquals(employee.getId(), login.user().getId());
        assertEquals("EMPLOYEE", login.profile());
    }

    @Test
    void shouldRejectAnUnknownToken() {
        givenInvite(NOW.plusHours(72), null);

        assertThrows(InviteNotFoundException.class, () -> useCase.execute("token-inexistente", PASSWORD));

        assertNothingChanged();
    }

    @Test
    void shouldRejectAUsedTokenAsNotFound() {
        givenInvite(NOW.plusHours(72), NOW.minusHours(1));

        assertThrows(InviteNotFoundException.class, () -> useCase.execute(TOKEN, PASSWORD));

        assertNothingChanged();
    }

    @Test
    void shouldRejectTheTokenWhenAConcurrentRequestUsedItFirst() {
        givenInvite(NOW.plusHours(72), null);
        invites.simulateConcurrentUseAfterNextLookup(NOW.minusSeconds(1));

        assertThrows(InviteNotFoundException.class, () -> useCase.execute(TOKEN, PASSWORD));

        assertEquals(NOW.minusSeconds(1), storedInvite().getUsedAt());
        assertNothingChanged();
    }

    @Test
    void shouldRejectAnExpiredToken() {
        givenInvite(NOW.minusMinutes(1), null);

        assertThrows(InviteExpiredException.class, () -> useCase.execute(TOKEN, PASSWORD));

        assertFalse(storedInvite().isUsed());
        assertNothingChanged();
    }

    @Test
    void shouldRejectAPasswordShorterThanTheMinimum() {
        givenInvite(NOW.plusHours(72), null);
        final String shortPassword = "a".repeat(CreateUserUseCase.MIN_PASSWORD_LENGTH - 1);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(TOKEN, shortPassword));

        assertEquals("Password must be at least 8 characters", ex.getMessage());
        assertFalse(storedInvite().isUsed());
        assertNothingChanged();
    }

    private void givenInvite(LocalDateTime expiresAt, LocalDateTime usedAt) {
        invites.save(new AccountInvite(null, employee.getId(), COMPANY_ID, AccountInvite.hashToken(TOKEN),
                expiresAt, usedAt, NOW.minusDays(1)));
    }

    private AccountInvite storedInvite() {
        return invites.findByTokenHash(AccountInvite.hashToken(TOKEN)).orElseThrow();
    }

    private void assertNothingChanged() {
        final User stored = users.findById(employee.getId()).orElseThrow();
        assertEquals(UserStatus.PENDING, stored.getStatus());
        assertEquals(PENDING_HASH, stored.getPasswordHash());
    }
}
