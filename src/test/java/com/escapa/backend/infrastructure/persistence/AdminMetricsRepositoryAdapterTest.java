package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import com.escapa.backend.application.port.AdminMetricsRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(new EnrollmentCompletionCounts(3, 2), repository.countHistoricalCompletion());
    }

    @Test
    void shouldReturnZerosForEmptyDatabase() {
        assertEquals(0, repository.countActiveStudents(TODAY));
        assertEquals(0, repository.countPartnerCompanies());
        assertEquals(new EnrollmentCompletionCounts(0, 0), repository.countHistoricalCompletion());
    }

    private UUID user(String role, String status) {
        final UUID id = UUID.randomUUID();
        jdbc.update("insert into users (id, name, email, password_hash, role, status) "
                + "values (?, 'Metrics Test', ?, 'hash', ?, ?)", id, id + "@example.com", role, status);
        return id;
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
