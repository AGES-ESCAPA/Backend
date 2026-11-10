package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.CompanyEmployeeCounts;
import com.escapa.backend.application.model.CompanySeatsSummary;
import com.escapa.backend.application.model.StudentContact;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeUnlinkPort;
import com.escapa.backend.application.port.CompanySeatsPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnlinkCompanyEmployeeUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");

    private final UUID companyA = UUID.randomUUID();
    private final UUID companyB = UUID.randomUUID();
    private final UUID companyCourse = UUID.randomUUID();
    private final UUID otherCompanyCourse = UUID.randomUUID();
    private final UUID ownCourse = UUID.randomUUID();

    private World world;
    private UnlinkCompanyEmployeeUseCase useCase;

    @BeforeEach
    void setUp() {
        world = new World();
        world.seatsTotal.put(companyA, 10);
        world.seatsTotal.put(companyB, 3);
        world.companyCourses.computeIfAbsent(companyA, ignored -> new HashSet<>()).add(companyCourse);
        world.companyCourses.computeIfAbsent(companyB, ignored -> new HashSet<>()).add(otherCompanyCourse);

        final GetCompanySeatsSummaryUseCase summary = new GetCompanySeatsSummaryUseCase(world, world);
        useCase = new UnlinkCompanyEmployeeUseCase(
                world, world, summary, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldUnlinkAnActiveEmployee() {
        final UUID employee = world.employee(companyA, UserStatus.ACTIVE);
        world.enroll(employee, companyCourse, false);

        final CompanySeatsSummary summary = useCase.execute(companyA, employee);

        assertFalse(world.links.containsKey(employee));
        assertEquals(UserStatus.INACTIVE, world.users.get(employee).getStatus());
        assertEquals(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC), world.revokedAt(employee, companyCourse));
        assertEquals(new CompanySeatsSummary(10, 0, 10, 0, 0), summary);
    }

    @Test
    void shouldFreeTheSeatOfTheEmployee() {
        final UUID leaving = world.employee(companyA, UserStatus.ACTIVE);
        world.employee(companyA, UserStatus.ACTIVE);
        world.employee(companyA, UserStatus.PENDING);

        final CompanySeatsSummary summary = useCase.execute(companyA, leaving);

        assertEquals(new CompanySeatsSummary(10, 2, 8, 2, 1), summary);
    }

    @Test
    void shouldUnlinkAPendingEmployeeAndInvalidateTheirInvites() {
        final UUID employee = world.employee(companyA, UserStatus.PENDING);
        world.pendingInvites.put(employee, 2);

        useCase.execute(companyA, employee);

        assertEquals(UserStatus.INACTIVE, world.users.get(employee).getStatus());
        assertEquals(0, world.pendingInvites.get(employee));
        assertFalse(world.links.containsKey(employee));
    }

    @Test
    void shouldUnlinkAnEmployeeWhoIsAlreadyInactive() {
        final UUID employee = world.employee(companyA, UserStatus.INACTIVE);
        world.enroll(employee, companyCourse, false);

        final CompanySeatsSummary summary = useCase.execute(companyA, employee);

        assertEquals(UserStatus.INACTIVE, world.users.get(employee).getStatus());
        assertNotNull(world.revokedAt(employee, companyCourse));
        assertEquals(0, summary.seatsUsed());
        // Já estava inativo: a conta não precisa ser salva de novo.
        assertEquals(0, world.userSaves);
    }

    @Test
    void shouldKeepTheCoursesTheEmployeeBoughtOnTheirOwn() {
        final UUID employee = world.employee(companyA, UserStatus.ACTIVE);
        world.enroll(employee, companyCourse, false);
        world.enroll(employee, ownCourse, false);
        world.selfBought.add(employee + ":" + ownCourse);
        // Curso da empresa que o colaborador também comprou por conta própria continua dele.
        world.companyCourses.get(companyA).add(ownCourse);

        useCase.execute(companyA, employee);

        assertNotNull(world.revokedAt(employee, companyCourse));
        assertNull(world.revokedAt(employee, ownCourse));
    }

    @Test
    void shouldNotTouchEnrollmentsInCoursesOfAnotherCompany() {
        final UUID employee = world.employee(companyA, UserStatus.ACTIVE);
        world.enroll(employee, otherCompanyCourse, false);

        useCase.execute(companyA, employee);

        assertNull(world.revokedAt(employee, otherCompanyCourse));
    }

    @Test
    void shouldNotRevokeEnrollmentsOfOtherEmployees() {
        final UUID leaving = world.employee(companyA, UserStatus.ACTIVE);
        final UUID staying = world.employee(companyA, UserStatus.ACTIVE);
        world.enroll(leaving, companyCourse, false);
        world.enroll(staying, companyCourse, false);

        useCase.execute(companyA, leaving);

        assertNotNull(world.revokedAt(leaving, companyCourse));
        assertNull(world.revokedAt(staying, companyCourse));
        assertEquals(UserStatus.ACTIVE, world.users.get(staying).getStatus());
    }

    @Test
    void shouldKeepTheEnrollmentsAndTheirProgress() {
        final UUID employee = world.employee(companyA, UserStatus.ACTIVE);
        world.enroll(employee, companyCourse, false);

        useCase.execute(companyA, employee);

        // Revogar é só preencher revoked_at: a matrícula continua existindo.
        assertEquals(1, world.enrollments.size());
    }

    @Test
    void shouldRefuseAnEmployeeOfAnotherCompanyWithoutChangingAnything() {
        final UUID outsider = world.employee(companyB, UserStatus.ACTIVE);
        world.enroll(outsider, otherCompanyCourse, false);
        world.pendingInvites.put(outsider, 1);

        assertThrows(CompanyEmployeeNotFoundException.class, () -> useCase.execute(companyA, outsider));

        assertTrue(world.links.containsKey(outsider));
        assertEquals(UserStatus.ACTIVE, world.users.get(outsider).getStatus());
        assertNull(world.revokedAt(outsider, otherCompanyCourse));
        assertEquals(1, world.pendingInvites.get(outsider));
    }

    @Test
    void shouldRefuseAnUnknownEmployee() {
        assertThrows(CompanyEmployeeNotFoundException.class, () -> useCase.execute(companyA, UUID.randomUUID()));
    }

    @Test
    void shouldRefuseUnlinkingTheSameEmployeeTwice() {
        final UUID employee = world.employee(companyA, UserStatus.ACTIVE);
        useCase.execute(companyA, employee);

        assertThrows(CompanyEmployeeNotFoundException.class, () -> useCase.execute(companyA, employee));
    }

    @Test
    void shouldPropagateAFailureOfAnyStep() {
        final UUID employee = world.employee(companyA, UserStatus.ACTIVE);
        world.failOnInvalidatingInvites = true;

        // A transação do caso de uso desfaz o que já foi feito; aqui basta a falha chegar a quem chamou.
        assertThrows(IllegalStateException.class, () -> useCase.execute(companyA, employee));
    }

    @Test
    void shouldRefuseMissingIds() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(companyA, null));
    }

    private record Enrollment(UUID userId, UUID courseId, LocalDateTime[] revokedAt) {
    }

    /** Empresas, colaboradores, matrículas e convites em memória, atrás de todas as portas usadas. */
    private static final class World
            implements CompanySeatsPort, CompanyEmployeeRepositoryPort, CompanyEmployeeUnlinkPort, UserRepositoryPort {

        private final Map<UUID, Integer> seatsTotal = new HashMap<>();
        private final Map<UUID, UUID> links = new HashMap<>();
        private final Map<UUID, User> users = new HashMap<>();
        private final Map<UUID, Set<UUID>> companyCourses = new HashMap<>();
        private final List<Enrollment> enrollments = new ArrayList<>();
        private final Set<String> selfBought = new HashSet<>();
        private final Map<UUID, Integer> pendingInvites = new HashMap<>();
        private boolean failOnInvalidatingInvites;
        private int userSaves;

        private UUID employee(UUID company, UserStatus status) {
            final UUID id = UUID.randomUUID();
            users.put(id, new User(id, "Colaborador", id + "@empresa.com.br", "hash", "STUDENT", null, status, null));
            links.put(id, company);
            return id;
        }

        private void enroll(UUID userId, UUID courseId, boolean revoked) {
            enrollments.add(new Enrollment(userId, courseId, new LocalDateTime[] {revoked ? LocalDateTime.MIN : null}));
        }

        private LocalDateTime revokedAt(UUID userId, UUID courseId) {
            return enrollments.stream()
                    .filter(item -> item.userId().equals(userId) && item.courseId().equals(courseId))
                    .findFirst()
                    .map(item -> item.revokedAt()[0])
                    .orElse(null);
        }

        @Override
        public boolean removeLink(UUID companyId, UUID employeeId) {
            return links.remove(employeeId, companyId);
        }

        @Override
        public int revokeCompanyCourseEnrollments(UUID companyId, UUID employeeId, LocalDateTime revokedAt) {
            int revoked = 0;
            for (final Enrollment item : enrollments) {
                final boolean ofTheEmployee = item.userId().equals(employeeId) && item.revokedAt()[0] == null;
                final boolean ofTheCompany = companyCourses.getOrDefault(companyId, Set.of()).contains(item.courseId());
                if (ofTheEmployee && ofTheCompany && !selfBought.contains(employeeId + ":" + item.courseId())) {
                    item.revokedAt()[0] = revokedAt;
                    revoked++;
                }
            }
            return revoked;
        }

        @Override
        public int invalidatePendingInvites(UUID companyId, UUID employeeId, LocalDateTime now) {
            if (failOnInvalidatingInvites) {
                throw new IllegalStateException("boom");
            }
            final Integer before = pendingInvites.replace(employeeId, 0);
            return before == null ? 0 : before;
        }

        @Override
        public int findSeatsTotal(UUID companyId) {
            return seatsTotal.get(companyId);
        }

        @Override
        public int countSeatsInUse(UUID companyId) {
            return (int) links.values().stream().filter(companyId::equals).count();
        }

        @Override
        public void updateSeatsTotal(UUID companyId, int total) {
            seatsTotal.put(companyId, total);
        }

        @Override
        public boolean existsCompany(UUID companyId) {
            return seatsTotal.containsKey(companyId);
        }

        @Override
        public CompanyEmployeeCounts countEmployees(UUID companyId) {
            final List<UUID> members = links.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(companyId))
                    .map(Map.Entry::getKey)
                    .toList();
            final int active = (int) members.stream()
                    .filter(id -> users.get(id).getStatus() == UserStatus.ACTIVE)
                    .count();
            return new CompanyEmployeeCounts(members.size(), active);
        }

        @Override
        public PageResult<CompanyEmployee> findEmployees(UUID companyId, String search, int page, int size) {
            throw new UnsupportedOperationException("not used by the unlink");
        }

        @Override
        public User save(User user) {
            userSaves++;
            users.put(user.getId(), user);
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public boolean existsByEmail(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<User> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isUserLinkedToCompany(UUID userId) {
            return links.containsKey(userId);
        }

        @Override
        public void updatePasswordHash(UUID id, String passwordHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<StudentContact> findStudentContact(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateStudentPhone(UUID userId, String phone) {
            throw new UnsupportedOperationException();
        }
    }
}
