package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.application.port.CompanyEmployeeUnlinkPort;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;
import com.escapa.backend.domain.course.CourseNotFoundException;

class RevokeEmployeeCourseUseCaseTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 12, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();

    private final FakeEmployeeLinks links = new FakeEmployeeLinks();
    private final FakeAcquiredCourses courses = new FakeAcquiredCourses();
    private final RecordingUnlinkPort unlinkPort = new RecordingUnlinkPort();
    private final RevokeEmployeeCourseUseCase useCase = new RevokeEmployeeCourseUseCase(
            links, courses, unlinkPort, CLOCK);

    @BeforeEach
    void setUp() {
        links.linkEmployee(COMPANY_ID, EMPLOYEE_ID, "Analista");
        courses.add(COURSE_ID, 30);
    }

    @Test
    void shouldRevokeTheCorporateEnrollment() {
        useCase.execute(COMPANY_ID, EMPLOYEE_ID, COURSE_ID);

        assertTrue(unlinkPort.revoked.contains(EMPLOYEE_ID + ":" + COURSE_ID));
    }

    @Test
    void shouldRejectAnEmployeeOfAnotherCompany() {
        assertThrows(CompanyEmployeeNotFoundException.class,
                () -> useCase.execute(COMPANY_ID, UUID.randomUUID(), COURSE_ID));
        assertFalse(unlinkPort.revoked.contains(EMPLOYEE_ID + ":" + COURSE_ID));
    }

    @Test
    void shouldRejectACourseTheCompanyDoesNotOwn() {
        assertThrows(CourseNotFoundException.class,
                () -> useCase.execute(COMPANY_ID, EMPLOYEE_ID, UUID.randomUUID()));
    }

    @Test
    void shouldRejectNullArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(null, EMPLOYEE_ID, COURSE_ID));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(COMPANY_ID, null, COURSE_ID));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(COMPANY_ID, EMPLOYEE_ID, null));
    }

    private static final class RecordingUnlinkPort implements CompanyEmployeeUnlinkPort {
        private final Set<String> revoked = new HashSet<>();
        private final Map<UUID, Set<UUID>> unused = new HashMap<>();

        @Override
        public boolean removeLink(UUID companyId, UUID employeeId) {
            return false;
        }

        @Override
        public int revokeCompanyCourseEnrollments(UUID companyId, UUID employeeId, LocalDateTime revokedAt) {
            return 0;
        }

        @Override
        public boolean revokeCompanyCourseEnrollment(
                UUID companyId, UUID employeeId, UUID courseId, LocalDateTime revokedAt) {
            unused.computeIfAbsent(companyId, ignored -> new HashSet<>()).add(courseId);
            return revoked.add(employeeId + ":" + courseId);
        }

        @Override
        public int invalidatePendingInvites(UUID companyId, UUID employeeId, LocalDateTime now) {
            return 0;
        }
    }
}
