package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.RegisteredEmployee;
import com.escapa.backend.domain.company.CompanyNotFoundException;
import com.escapa.backend.domain.company.NoSeatsAvailableException;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterCompanyEmployeeUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final LocalDate TODAY = NOW.toLocalDate();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID COURSE_A = UUID.randomUUID();
    private static final UUID COURSE_B = UUID.randomUUID();
    private static final long INVITE_HOURS = 72;

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private final InMemoryCompanyRepositoryPort companies = new InMemoryCompanyRepositoryPort(users);
    private final FakeCompanySeatsPort seats = new FakeCompanySeatsPort();
    private final FakeEmployeeLinks links = new FakeEmployeeLinks();
    private final FakeAcquiredCourses courses = new FakeAcquiredCourses();
    private final RecordingOrderAccessPort enrollments = new RecordingOrderAccessPort();
    private final InMemoryAccountInviteRepositoryPort invites = new InMemoryAccountInviteRepositoryPort();
    private final RegisterCompanyEmployeeUseCase useCase = new RegisterCompanyEmployeeUseCase(
            users, companies, seats, links, courses, enrollments, invites,
            new FakePasswordHasher(), new FakeContactValidator(), CLOCK, INVITE_HOURS);

    @BeforeEach
    void setUp() {
        companies.save(new User(COMPANY_ID, "Hotel Gramado", "rh@hotel.com.br", "x", "COMPANY", null, NOW),
                new Company(COMPANY_ID, "Hotel Gramado LTDA", "11222333000144", "rh@hotel.com.br", "51999990001"));
        seats.givenCompany(COMPANY_ID, 5, 2);
        courses.add(COURSE_A, 30);
        courses.add(COURSE_B, null);
    }

    @Test
    void shouldRegisterAPendingEmployeeLinkedAndEnrolledInTheCompanyCourses() {
        final RegisteredEmployee registered = useCase.execute(COMPANY_ID, "João da Silva", "joao.silva@empresa.com.br");

        final User stored = users.findByEmail("joao.silva@empresa.com.br").orElseThrow();
        assertEquals(registered.employee().getId(), stored.getId());
        assertEquals("João da Silva", stored.getName());
        assertEquals("STUDENT", stored.getUserType());
        assertEquals(UserStatus.PENDING, stored.getStatus());
        assertTrue(links.isEmployeeOfCompany(COMPANY_ID, stored.getId()));
        assertEquals(2, registered.assignedCourses());
        assertEquals(new Enrollment(TODAY, TODAY.plusDays(30)), enrollments.enrollment(stored.getId(), COURSE_A));
        assertEquals(new Enrollment(TODAY, null), enrollments.enrollment(stored.getId(), COURSE_B));
        assertEquals("Hotel Gramado LTDA", registered.companyName());
    }

    @Test
    void shouldStoreTheInviteWithOnlyTheHashOfTheReturnedToken() {
        final RegisteredEmployee registered = useCase.execute(COMPANY_ID, "João da Silva", "joao.silva@empresa.com.br");

        final AccountInvite invite = invites.findByTokenHash(AccountInvite.hashToken(registered.inviteToken()))
                .orElseThrow();
        assertEquals(registered.employee().getId(), invite.getUserId());
        assertEquals(COMPANY_ID, invite.getCompanyId());
        assertEquals(NOW.plusHours(INVITE_HOURS), invite.getExpiresAt());
        assertNull(invite.getUsedAt());
        assertNotEquals(registered.inviteToken(), invite.getTokenHash());
    }

    @Test
    void shouldCreateTheAccountWithAnUnusablePassword() {
        final RegisteredEmployee first = useCase.execute(COMPANY_ID, "João", "joao@empresa.com.br");
        final RegisteredEmployee second = useCase.execute(COMPANY_ID, "Maria", "maria@empresa.com.br");

        assertNotNull(first.employee().getPasswordHash());
        assertNotEquals(first.employee().getPasswordHash(), second.employee().getPasswordHash());
    }

    @Test
    void shouldTrimTheNameAndNormalizeTheEmail() {
        final RegisteredEmployee registered = useCase.execute(COMPANY_ID, "  João da Silva ", "  Joao.Silva@Empresa.COM.br ");

        assertEquals("João da Silva", registered.employee().getName());
        assertEquals("joao.silva@empresa.com.br", registered.employee().getEmail());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRefuseAMissingName(String name) {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, name, "joao@empresa.com.br"));
        assertNothingWasCreated();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "sem-arroba", "joao@", "joao silva@empresa.com.br"})
    void shouldRefuseAMissingOrInvalidEmail(String email) {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(COMPANY_ID, "João", email));
        assertNothingWasCreated();
    }

    @Test
    void shouldRefuseWhenThereIsNoFreeSeatAndCreateNothing() {
        seats.givenCompany(COMPANY_ID, 5, 5);

        assertThrows(NoSeatsAvailableException.class,
                () -> useCase.execute(COMPANY_ID, "João", "joao@empresa.com.br"));

        assertNothingWasCreated();
    }

    @Test
    void shouldUseTheLastFreeSeat() {
        seats.givenCompany(COMPANY_ID, 5, 4);

        useCase.execute(COMPANY_ID, "João", "joao@empresa.com.br");

        assertEquals(1, links.count());
    }

    @Test
    void shouldRefuseAnEmailThatBelongsToAnActiveAccount() {
        users.save(new User(null, "Ana", "ana@empresa.com.br", "h", "STUDENT", null, UserStatus.ACTIVE, NOW));

        assertThrows(DuplicateRegistrationException.class,
                () -> useCase.execute(COMPANY_ID, "Ana Paula", "ANA@empresa.com.br"));

        assertNothingWasCreated();
    }

    @Test
    void shouldRefuseAnEmailThatIsAlreadyPending() {
        users.save(new User(null, "Ana", "ana@empresa.com.br", "h", "STUDENT", null, UserStatus.PENDING, NOW));

        assertThrows(DuplicateRegistrationException.class,
                () -> useCase.execute(COMPANY_ID, "Ana", "ana@empresa.com.br"));
    }

    @Test
    void shouldRefuseAnInactiveAccountThatWasNeverAnEmployee() {
        users.save(new User(null, "Ana", "ana@empresa.com.br", "h", "STUDENT", null, UserStatus.INACTIVE, NOW));

        assertThrows(DuplicateRegistrationException.class,
                () -> useCase.execute(COMPANY_ID, "Ana", "ana@empresa.com.br"));
    }

    @Test
    void shouldRefuseAnInactiveFormerEmployeeThatIsStillLinkedToACompany() {
        final User former = formerEmployee();
        users.linkUserToCompany(former.getId());

        assertThrows(DuplicateRegistrationException.class,
                () -> useCase.execute(COMPANY_ID, "Ana", "ana@empresa.com.br"));
    }

    @Test
    void shouldReactivateAFormerEmployeeInsteadOfCreatingAnotherAccount() {
        final User former = formerEmployee();

        final RegisteredEmployee registered = useCase.execute(COMPANY_ID, "Ana Paula", "ana@empresa.com.br");

        assertEquals(former.getId(), registered.employee().getId());
        assertEquals(1, users.findAll().stream().filter(user -> "ana@empresa.com.br".equals(user.getEmail())).count());
        final User stored = users.findById(former.getId()).orElseThrow();
        assertEquals(UserStatus.PENDING, stored.getStatus());
        assertNotEquals("old-password-hash", stored.getPasswordHash());
        assertTrue(links.isEmployeeOfCompany(COMPANY_ID, former.getId()));
        assertEquals(new Enrollment(TODAY, TODAY.plusDays(30)), enrollments.enrollment(former.getId(), COURSE_A));
        assertTrue(invites.findByTokenHash(AccountInvite.hashToken(registered.inviteToken())).isPresent());
    }

    @Test
    void shouldFailForACompanyThatDoesNotExist() {
        assertThrows(CompanyNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), "João", "joao@empresa.com.br"));
    }

    private User formerEmployee() {
        final User former = users.save(new User(null, "Ana", "ana@empresa.com.br", "old-password-hash",
                "STUDENT", null, UserStatus.INACTIVE, NOW.minusDays(90)));
        invites.save(AccountInvite.issue(former.getId(), UUID.randomUUID(), "old-hash", NOW.minusDays(90), 72));
        return former;
    }

    private void assertNothingWasCreated() {
        assertEquals(0, links.count());
        assertEquals(0, enrollments.enrollmentCount());
        assertTrue(users.findAll().stream().noneMatch(user -> user.getStatus() == UserStatus.PENDING));
    }
}
