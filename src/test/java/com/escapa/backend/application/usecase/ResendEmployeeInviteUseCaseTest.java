package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.IssuedEmployeeInvite;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.EmployeeNotPendingException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResendEmployeeInviteUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String OLD_TOKEN = "token-antigo";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companies = new InMemoryCompanyRepositoryPort(users);
    private final FakeEmployeeLinks links = new FakeEmployeeLinks();
    private final InMemoryAccountInviteRepositoryPort invites = new InMemoryAccountInviteRepositoryPort();
    private final ResendEmployeeInviteUseCase useCase =
            new ResendEmployeeInviteUseCase(users, companies, links, invites, CLOCK, 72);

    private User employee;

    @BeforeEach
    void setUp() {
        companies.save(new User(COMPANY_ID, "Hotel Gramado", "rh@hotel.com.br", "x", "COMPANY", null, NOW),
                new Company(COMPANY_ID, "Hotel Gramado LTDA", "11222333000144", "rh@hotel.com.br", "51999990001"));
        employee = users.save(new User(null, "João", "joao@empresa.com.br", "h", "STUDENT", null,
                UserStatus.PENDING, NOW.minusDays(1)));
        links.linkEmployee(COMPANY_ID, employee.getId(), "COLABORADOR");
    }

    @Test
    void shouldIssueANewInviteAndInvalidateTheOpenOne() {
        invites.save(AccountInvite.issue(employee.getId(), COMPANY_ID, AccountInvite.hashToken(OLD_TOKEN),
                NOW.minusHours(1), 72));

        final IssuedEmployeeInvite issued = useCase.execute(COMPANY_ID, employee.getId());

        assertNotEquals(OLD_TOKEN, issued.inviteToken());
        assertTrue(invites.findByTokenHash(AccountInvite.hashToken(OLD_TOKEN)).orElseThrow().isUsed());
        final AccountInvite fresh = invites.findByTokenHash(AccountInvite.hashToken(issued.inviteToken())).orElseThrow();
        assertFalse(fresh.isUsed());
        assertEquals(NOW.plusHours(72), fresh.getExpiresAt());
        assertEquals(employee.getId(), fresh.getUserId());
        assertEquals(COMPANY_ID, fresh.getCompanyId());
        assertEquals("Hotel Gramado LTDA", issued.companyName());
        assertEquals(employee.getEmail(), issued.employee().getEmail());
    }

    @Test
    void shouldIssueANewInviteWhenThePreviousOneAlreadyExpired() {
        invites.save(AccountInvite.issue(employee.getId(), COMPANY_ID, AccountInvite.hashToken(OLD_TOKEN),
                NOW.minusDays(10), 72));

        final IssuedEmployeeInvite issued = useCase.execute(COMPANY_ID, employee.getId());

        assertTrue(invites.findByTokenHash(AccountInvite.hashToken(issued.inviteToken())).isPresent());
        assertNull(invites.findByTokenHash(AccountInvite.hashToken(issued.inviteToken())).orElseThrow().getUsedAt());
    }

    @Test
    void shouldIssueAnInviteWhenTheEmployeeHasNoneYet() {
        final IssuedEmployeeInvite issued = useCase.execute(COMPANY_ID, employee.getId());

        assertTrue(invites.findByTokenHash(AccountInvite.hashToken(issued.inviteToken())).isPresent());
    }

    @Test
    void shouldRefuseAnEmployeeThatIsNotPending() {
        employee.setStatus(UserStatus.ACTIVE);
        users.save(employee);

        assertThrows(EmployeeNotPendingException.class, () -> useCase.execute(COMPANY_ID, employee.getId()));
    }

    @Test
    void shouldRefuseAnEmployeeOfAnotherCompanyAsNotFound() {
        assertThrows(UserNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), employee.getId()));
    }

    @Test
    void shouldRefuseAnUnknownEmployeeAsNotFound() {
        assertThrows(UserNotFoundException.class, () -> useCase.execute(COMPANY_ID, UUID.randomUUID()));
    }
}
