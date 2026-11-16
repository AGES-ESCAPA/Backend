package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.AccountInviteDetails;
import com.escapa.backend.domain.entity.Company;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetAccountInviteUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final String TOKEN = "convite-joao-dev";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companies = new InMemoryCompanyRepositoryPort(users);
    private final InMemoryAccountInviteRepositoryPort invites = new InMemoryAccountInviteRepositoryPort();
    private final GetAccountInviteUseCase useCase = new GetAccountInviteUseCase(invites, users, companies, CLOCK);

    private UUID companyId;
    private User employee;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        companies.save(
                new User(companyId, "Marta Lima", "contato@vistamar.com.br", "hash", "COMPANY", null,
                        UserStatus.ACTIVE, NOW.minusDays(30)),
                new Company(companyId, "Pousada Vista Mar LTDA", "12345678000199", "contato@vistamar.com.br", null));
        employee = users.save(new User(null, "João da Silva", "joao.silva@empresa.com.br",
                "PENDING-NO-PASSWORD-SET", "STUDENT", null, UserStatus.PENDING, NOW.minusDays(1)));
    }

    @Test
    void shouldReturnTheEmployeeAndCompanyOfAValidInvite() {
        givenInvite(NOW.plusHours(72), null);

        final AccountInviteDetails details = useCase.execute(TOKEN);

        assertEquals("João da Silva", details.name());
        assertEquals("joao.silva@empresa.com.br", details.email());
        assertEquals("Pousada Vista Mar LTDA", details.companyName());
    }

    @Test
    void shouldRejectAnUnknownToken() {
        givenInvite(NOW.plusHours(72), null);

        assertThrows(InviteNotFoundException.class, () -> useCase.execute("token-inexistente"));
    }

    @Test
    void shouldRejectAUsedTokenAsNotFound() {
        givenInvite(NOW.plusHours(72), NOW.minusHours(1));

        assertThrows(InviteNotFoundException.class, () -> useCase.execute(TOKEN));
    }

    @Test
    void shouldRejectAnExpiredToken() {
        givenInvite(NOW.minusMinutes(1), null);

        assertThrows(InviteExpiredException.class, () -> useCase.execute(TOKEN));
    }

    @Test
    void shouldRejectABlankToken() {
        assertThrows(InviteNotFoundException.class, () -> useCase.execute(" "));
    }

    private void givenInvite(LocalDateTime expiresAt, LocalDateTime usedAt) {
        invites.save(new AccountInvite(null, employee.getId(), companyId, AccountInvite.hashToken(TOKEN),
                expiresAt, usedAt, NOW.minusDays(1)));
    }
}
