package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.escapa.backend.application.model.CompanyEngagement;
import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import com.escapa.backend.application.port.AdminMetricsRepositoryPort;

@Transactional
class AdminMetricsRepositoryAdapterTest extends PostgresIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

    @Autowired
    private AdminMetricsRepositoryPort repository;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldCountDistinctActiveStudentsWithInclusiveEnrollmentDates() {
        final UUID student = user("STUDENT", "ACTIVE");
        final UUID anotherStudent = user("STUDENT", "ACTIVE");
        final UUID invalidIntervalStudent = user("STUDENT", "ACTIVE");
        final UUID inactiveStudent = user("STUDENT", "INACTIVE");
        final UUID published = course("PUBLISHED");
        final UUID archived = course("ARCHIVED");
        final UUID draft = course("DRAFT");
        enrollment(student, published, TODAY, TODAY, null, null, null);
        enrollment(student, archived, null, null, null, null, null);
        enrollment(anotherStudent, published, null, TODAY.minusDays(1), null, null, null);
        enrollment(anotherStudent, archived, TODAY.plusDays(1), null, null, null, null);
        enrollment(anotherStudent, draft, null, null, null, null, null);
        final UUID secondPublished = course("PUBLISHED");
        enrollment(anotherStudent, secondPublished, TODAY.plusDays(1), null, null, null, null);
        enrollment(invalidIntervalStudent, published, TODAY.plusDays(2), TODAY.plusDays(1), null, null, null);
        enrollment(inactiveStudent, published, null, null, null, null, null);

        assertEquals(1, repository.countActiveStudents(TODAY));
        assertEquals(2, repository.countActiveStudentsInPeriod(TODAY, TODAY.plusDays(1)));
        assertEquals(2, repository.countActiveStudentsInPeriod(TODAY.minusDays(1), TODAY));
    }

    @Test
    void shouldExcludeRevokedAndDraftEnrollmentsAndCountCompaniesByStatus() {
        final UUID student = user("STUDENT", "ACTIVE");
        final UUID published = course("PUBLISHED");
        final UUID draft = course("DRAFT");
        enrollment(student, published, null, null, null, null, TODAY.atStartOfDay());
        enrollment(student, draft, null, null, null, null, null);
        user("COMPANY", "ACTIVE");
        user("COMPANY", "INACTIVE");
        user("COMPANY", "PENDING");

        assertEquals(0, repository.countActiveStudents(TODAY));
        assertEquals(1, repository.countPartnerCompanies());
    }

    @Test
    void shouldCountHistoricalCompletionIncludingExpiredEnrollments() {
        final UUID student = user("STUDENT", "ACTIVE");
        final UUID other = user("STUDENT", "INACTIVE");
        final UUID published = course("PUBLISHED");
        final UUID archived = course("ARCHIVED");
        final UUID draft = course("DRAFT");
        enrollment(student, published, null, TODAY.minusDays(1), 100, null, null);
        enrollment(other, published, null, null, 10, TODAY, null);
        enrollment(student, archived, null, null, 50, null, null);
        enrollment(student, draft, null, null, 100, null, null);
        enrollment(other, archived, null, null, 100, null, TODAY.atStartOfDay());

        assertEquals(new EnrollmentCompletionCounts(2, 2), repository.countHistoricalCompletion());
    }

    @Test
    void shouldReturnZerosForEmptyDatabase() {
        assertEquals(0, repository.countActiveStudents(TODAY));
        assertEquals(0, repository.countPartnerCompanies());
        assertEquals(0, repository.countPartnerCompaniesCreatedBefore(TODAY.withDayOfMonth(1)));
        assertEquals(new EnrollmentCompletionCounts(0, 0), repository.countHistoricalCompletion());
        assertEquals(List.of(), repository.findTopEngagedCompanies(5));
    }

    @Test
    void shouldRankActiveCompaniesByEligibleEmployeeEnrollments() {
        final LocalDateTime previousMonth = LocalDateTime.of(2026, 10, 15, 9, 0);
        final UUID alpha = company("Alpha", "ACTIVE", previousMonth);
        final UUID beta = company("Beta", "ACTIVE", LocalDateTime.of(2026, 11, 2, 8, 0));
        final UUID gamma = company("Gamma", "INACTIVE", LocalDateTime.of(2026, 9, 1, 8, 0));
        final UUID delta = company("Delta", "ACTIVE", LocalDateTime.of(2026, 10, 20, 8, 0));
        company("Epsilon", "ACTIVE", LocalDateTime.of(2026, 11, 1, 0, 0));
        final UUID published = course("PUBLISHED");
        final UUID draft = course("DRAFT");
        final UUID alphaEmployee = employeeOf(alpha);
        enrollment(alphaEmployee, published, null, null, null, null, null);
        enrollment(employeeOf(alpha), published, null, null, null, null, null);
        enrollment(alphaEmployee, draft, null, null, null, null, null);
        enrollment(employeeOf(beta), published, null, null, null, null, null);
        enrollment(employeeOf(gamma), published, null, null, null, null, null);
        enrollment(employeeOf(delta), published, null, null, null, null, TODAY.atStartOfDay());

        assertEquals(4, repository.countPartnerCompanies());
        assertEquals(2, repository.countPartnerCompaniesCreatedBefore(TODAY.withDayOfMonth(1)));
        assertEquals(List.of(
                new CompanyEngagement(alpha, "Alpha", 2),
                new CompanyEngagement(beta, "Beta", 1)), repository.findTopEngagedCompanies(5));
        assertEquals(List.of(new CompanyEngagement(alpha, "Alpha", 2)), repository.findTopEngagedCompanies(1));
    }

    private UUID user(String role, String status) {
        final UUID id = UUID.randomUUID();
        jdbc.update("insert into users (id, name, email, password_hash, role, status) "
                + "values (?, 'Metrics Test', ?, 'hash', ?, ?)", id, id + "@example.com", role, status);
        return id;
    }

    private UUID company(String name, String status, LocalDateTime createdAt) {
        final UUID id = UUID.randomUUID();
        jdbc.update("insert into users (id, name, email, password_hash, role, status, created_at) "
                + "values (?, ?, ?, 'hash', 'COMPANY', ?, ?)", id, name, id + "@example.com", status, createdAt);
        jdbc.update("insert into company (id, company_name) values (?, ?)", id, name);
        return id;
    }

    private UUID employeeOf(UUID companyId) {
        final UUID employeeId = user("STUDENT", "ACTIVE");
        jdbc.update("insert into users_company (user_id, company_id, role) values (?, ?, 'COLABORADOR')",
                employeeId, companyId);
        return employeeId;
    }

    private UUID course(String status) {
        final UUID id = UUID.randomUUID();
        jdbc.update("insert into courses (id, title, status) values (?, 'Metrics Test', ?)", id, status);
        return id;
    }

    private void enrollment(UUID userId, UUID courseId, LocalDate from, LocalDate to, Integer progress,
            LocalDate conclusion, LocalDateTime revoked) {
        jdbc.update("insert into user_courses (user_id, course_id, dt_inicio, dt_expiracao, progress, "
                        + "conclusion_date, revoked_at) values (?, ?, ?, ?, ?, ?, ?)",
                userId, courseId, from, to, progress, conclusion, revoked);
    }
}
