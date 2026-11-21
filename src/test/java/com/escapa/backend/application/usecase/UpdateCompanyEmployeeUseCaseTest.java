package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.escapa.backend.application.model.UpdatedEmployee;
import com.escapa.backend.application.usecase.UpdateCompanyEmployeeUseCase.EmployeeChanges;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.UserStatus;

class UpdateCompanyEmployeeUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID OTHER_COMPANY_ID = UUID.randomUUID();
    private static final String EMAIL = "ana@empresa.com.br";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companies = new InMemoryCompanyRepositoryPort(users);
    private final FakeEmployeeLinks links = new FakeEmployeeLinks();
    private final InMemoryAccountInviteRepositoryPort invites = new InMemoryAccountInviteRepositoryPort();
    private final UpdateCompanyEmployeeUseCase useCase = new UpdateCompanyEmployeeUseCase(
            users, companies, links, invites, new FakeContactValidator(), CLOCK, 72);

    @BeforeEach
    void setUp() {
        companies.save(new User(COMPANY_ID, "Pousada", "rh@pousada.com.br", "x", "COMPANY", null, NOW),
                new Company(COMPANY_ID, "Pousada Vista Mar LTDA", "11222333000144", "rh@pousada.com.br", "51999990001"));
    }

    private User givenEmployee(UUID companyId, UserStatus status) {
        final User employee = users.save(new User(null, "Ana", EMAIL, "hash", "STUDENT", null, status, NOW.minusDays(5)));
        links.linkEmployee(companyId, employee.getId(), "COLABORADOR");
        return employee;
    }

    private AccountInvite givenInvite(User employee, LocalDateTime issuedAt, boolean used) {
        final AccountInvite invite = AccountInvite.issue(employee.getId(), COMPANY_ID,
                AccountInvite.hashToken(UUID.randomUUID().toString()), issuedAt, 72);
        if (used) {
            invite.markAsUsed(issuedAt.plusHours(1));
        }
        return invites.save(invite);
    }

    private UpdatedEmployee update(User employee, String email, UserStatus status) {
        return useCase.execute(COMPANY_ID, employee.getId(),
                new EmployeeChanges("Ana Paula Ferreira", email, "Recepção", status));
    }

    private List<AccountInvite> openInvites(User employee) {
        return invites.invitesOf(employee.getId()).stream()
                .filter(invite -> !invite.isUsed() && !invite.isExpired(NOW))
                .toList();
    }

    @Test
    void shouldSaveNameEmailJobTitleAndStatusAndReturnTheUpdatedEmployee() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);

        final UpdatedEmployee updated = useCase.execute(COMPANY_ID, employee.getId(),
                new EmployeeChanges("  Ana Paula Ferreira ", " ANA.P@Empresa.com.br ", " Recepção ", UserStatus.ACTIVE));

        assertEquals("Ana Paula Ferreira", updated.employee().name());
        assertEquals("ana.p@empresa.com.br", updated.employee().email());
        assertEquals("Recepção", updated.employee().jobTitle());
        assertEquals(UserStatus.ACTIVE, updated.employee().status());
        assertEquals(2, updated.employee().assignedCourses());
        assertEquals("https://cdn/avatar.png", updated.employee().avatarUrl());
        assertFalse(updated.hasNewInvite());
        final User saved = users.findById(employee.getId()).orElseThrow();
        assertEquals("Ana Paula Ferreira", saved.getName());
        assertEquals("ana.p@empresa.com.br", saved.getEmail());
        assertEquals("Recepção", links.jobTitle(COMPANY_ID, employee.getId()));
    }

    @Test
    void shouldInactivateAnActiveEmployeeKeepingTheLink() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);

        final UpdatedEmployee updated = update(employee, EMAIL, UserStatus.INACTIVE);

        assertEquals(UserStatus.INACTIVE, updated.employee().status());
        assertEquals(UserStatus.INACTIVE, users.findById(employee.getId()).orElseThrow().getStatus());
        assertTrue(links.isEmployeeOfCompany(COMPANY_ID, employee.getId()));
        assertFalse(updated.hasNewInvite());
    }

    @Test
    void shouldReactivateAnEmployeeThatHadCreatedThePassword() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.INACTIVE);
        givenInvite(employee, NOW.minusDays(10), true);

        final UpdatedEmployee updated = update(employee, EMAIL, UserStatus.ACTIVE);

        assertEquals(UserStatus.ACTIVE, updated.employee().status());
        assertEquals(UserStatus.ACTIVE, users.findById(employee.getId()).orElseThrow().getStatus());
        assertFalse(updated.hasNewInvite());
    }

    @Test
    void shouldKeepAPendingEmployeePendingWhenSetActiveWithoutIssuingAnotherInvite() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.PENDING);
        final AccountInvite open = givenInvite(employee, NOW.minusHours(2), false);

        final UpdatedEmployee updated = update(employee, EMAIL, UserStatus.ACTIVE);

        assertEquals(UserStatus.PENDING, updated.employee().status());
        assertEquals(UserStatus.PENDING, users.findById(employee.getId()).orElseThrow().getStatus());
        assertFalse(updated.hasNewInvite());
        assertEquals(List.of(open.getId()), openInvites(employee).stream().map(AccountInvite::getId).toList());
    }

    @Test
    void shouldInvalidateTheOpenInviteWhenAPendingEmployeeIsInactivated() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.PENDING);
        givenInvite(employee, NOW.minusHours(2), false);

        final UpdatedEmployee updated = update(employee, EMAIL, UserStatus.INACTIVE);

        assertEquals(UserStatus.INACTIVE, updated.employee().status());
        assertTrue(openInvites(employee).isEmpty());
        assertFalse(updated.hasNewInvite());
    }

    @Test
    void shouldSendANewInviteAndInvalidateThePreviousOneWhenTheEmailOfAPendingEmployeeChanges() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.PENDING);
        final AccountInvite previous = givenInvite(employee, NOW.minusHours(2), false);

        final UpdatedEmployee updated = update(employee, "nova@empresa.com.br", UserStatus.ACTIVE);

        assertEquals(UserStatus.PENDING, updated.employee().status());
        assertTrue(updated.hasNewInvite());
        assertNotNull(updated.inviteToken());
        assertEquals("Pousada Vista Mar LTDA", updated.companyName());
        final List<AccountInvite> open = openInvites(employee);
        assertEquals(1, open.size());
        assertEquals(AccountInvite.hashToken(updated.inviteToken()), open.get(0).getTokenHash());
        assertFalse(open.get(0).getId().equals(previous.getId()));
    }

    @Test
    void shouldNotIssueAnInviteWhenTheEmailOfAPendingEmployeeChangesAndTheyAreInactivated() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.PENDING);
        givenInvite(employee, NOW.minusHours(2), false);

        final UpdatedEmployee updated = update(employee, "nova@empresa.com.br", UserStatus.INACTIVE);

        assertFalse(updated.hasNewInvite());
        assertTrue(openInvites(employee).isEmpty());
    }

    @Test
    void shouldNotIssueAnInviteWhenTheEmailOfAnActiveEmployeeChanges() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);

        final UpdatedEmployee updated = update(employee, "nova@empresa.com.br", UserStatus.ACTIVE);

        assertFalse(updated.hasNewInvite());
        assertNull(updated.companyName());
        assertEquals("nova@empresa.com.br", users.findById(employee.getId()).orElseThrow().getEmail());
    }

    @Test
    void shouldReturnAnInactivatedEmployeeWithoutPasswordToPendingWithANewInvite() {
        // Inativado ainda pendente: o último convite foi encerrado sem uso e a conta não tem senha.
        final User employee = givenEmployee(COMPANY_ID, UserStatus.INACTIVE);
        givenInvite(employee, NOW.minusDays(40), true);
        final AccountInvite closed = givenInvite(employee, NOW.minusDays(2), false);
        closed.expire(NOW.minusDays(1));
        invites.save(closed);

        final UpdatedEmployee updated = update(employee, EMAIL, UserStatus.ACTIVE);

        assertEquals(UserStatus.PENDING, updated.employee().status());
        assertTrue(updated.hasNewInvite());
        assertEquals(1, openInvites(employee).size());
    }

    @Test
    void shouldAcceptTheEmployeeKeepingTheirOwnEmail() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);

        final UpdatedEmployee updated = update(employee, EMAIL.toUpperCase(), UserStatus.ACTIVE);

        assertEquals(EMAIL, updated.employee().email());
    }

    @Test
    void shouldRefuseAnEmailInUseByAnotherAccount() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);
        users.save(new User(null, "Outra", "outra@empresa.com.br", "h", "STUDENT", null, UserStatus.ACTIVE, NOW));

        assertThrows(DuplicateRegistrationException.class,
                () -> update(employee, "outra@empresa.com.br", UserStatus.ACTIVE));

        assertEquals(EMAIL, users.findById(employee.getId()).orElseThrow().getEmail());
        assertEquals("COLABORADOR", links.jobTitle(COMPANY_ID, employee.getId()));
    }

    @Test
    void shouldTreatAnEmployeeOfAnotherCompanyAsNotFound() {
        final User outsider = givenEmployee(OTHER_COMPANY_ID, UserStatus.ACTIVE);

        assertThrows(CompanyEmployeeNotFoundException.class, () -> update(outsider, EMAIL, UserStatus.INACTIVE));

        assertEquals(UserStatus.ACTIVE, users.findById(outsider.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldTreatAnUnknownEmployeeAsNotFound() {
        assertThrows(CompanyEmployeeNotFoundException.class, () -> useCase.execute(COMPANY_ID, UUID.randomUUID(),
                new EmployeeChanges("Ana", EMAIL, "Recepção", UserStatus.ACTIVE)));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = "PENDING")
    void shouldRefuseAStatusTheManagerCannotChoose(UserStatus status) {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.PENDING);

        assertThrows(IllegalArgumentException.class, () -> update(employee, EMAIL, status));
    }

    @Test
    void shouldRequireTheStatus() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> update(employee, EMAIL, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRequireNameAndJobTitle(String blank) {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, employee.getId(),
                new EmployeeChanges(blank, EMAIL, "Recepção", UserStatus.ACTIVE)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, employee.getId(),
                new EmployeeChanges("Ana", EMAIL, blank, UserStatus.ACTIVE)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ana", "ana@", "ana @empresa.com.br"})
    void shouldRefuseAnInvalidEmail(String email) {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);

        assertThrows(IllegalArgumentException.class, () -> update(employee, email, UserStatus.ACTIVE));
    }

    @Test
    void shouldRefuseFieldsLongerThanTheColumn() {
        final User employee = givenEmployee(COMPANY_ID, UserStatus.ACTIVE);
        final String tooLong = "a".repeat(RegisterStudentUseCase.MAX_FIELD_LENGTH + 1);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, employee.getId(),
                new EmployeeChanges(tooLong, EMAIL, "Recepção", UserStatus.ACTIVE)));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, employee.getId(),
                new EmployeeChanges("Ana", EMAIL, tooLong, UserStatus.ACTIVE)));
    }
}
