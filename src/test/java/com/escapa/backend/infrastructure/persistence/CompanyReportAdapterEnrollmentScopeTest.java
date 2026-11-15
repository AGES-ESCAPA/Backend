package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CompanyReportFigures;
import com.escapa.backend.application.model.CompanyReportFigures.JobTitleFigures;
import com.escapa.backend.application.model.CompanyReportPeriod;
import com.escapa.backend.application.port.CompanyReportRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CompanyReportAdapterEnrollmentScopeTest extends PostgresIntegrationTest {

    private static final CompanyReportPeriod PERIOD = new CompanyReportPeriod(
            LocalDateTime.of(2026, 11, 2, 18, 0),
            LocalDateTime.of(2026, 10, 1, 0, 0),
            LocalDate.of(2026, 10, 31),
            LocalDateTime.of(2026, 6, 1, 0, 0));
    private static final LocalDate START = LocalDate.of(2026, 3, 1);
    private static final LocalDateTime DONE_AT = LocalDateTime.of(2026, 9, 1, 10, 0);

    @Autowired
    private CompanyReportRepositoryPort repository;
    @Autowired
    private JdbcTemplate jdbc;

    private CompanyReportFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new CompanyReportFixture(jdbc);
    }

    @Test
    void existsCompanyOnlyForRegisteredCompanies() {
        final UUID company = fixture.company("Alpha");

        assertTrue(repository.existsCompany(company));
        assertFalse(repository.existsCompany(UUID.randomUUID()));
    }

    @Test
    void companyWithoutEmployeesHasZeroedFiguresAndEmptyLists() {
        final UUID company = fixture.company("Alpha");

        assertEquals(new CompanyReportFigures(0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of()),
                repository.loadFigures(company, PERIOD));
    }

    @Test
    void neverCountsEmployeesOrLessonsOfAnotherCompany() {
        final UUID company = fixture.company("Alpha");
        final UUID other = fixture.company("Beta");
        final UUID course = fixture.course("Shared course");
        fixture.ownCourse(company, course);
        fixture.ownCourse(other, course);
        final UUID tenMinutes = fixture.lesson(course, 10);
        final UUID twentyMinutes = fixture.lesson(course, 20);
        final UUID ana = fixture.employee(company, "Ana", "ACTIVE", "RECEPCAO");
        final UUID bia = fixture.employee(company, "Bia", "ACTIVE", "GERENCIA");
        final UUID zed = fixture.employee(other, "Zed", "ACTIVE", "RECEPCAO");
        fixture.enrollment(ana, course, START, 100, LocalDate.of(2026, 9, 1), null);
        fixture.enrollment(bia, course, START, 40, null, null);
        fixture.enrollment(zed, course, START, 100, LocalDate.of(2026, 9, 1), null);
        fixture.completed(ana, tenMinutes, DONE_AT);
        fixture.completed(bia, twentyMinutes, DONE_AT);
        fixture.completed(zed, tenMinutes, DONE_AT);
        fixture.completed(zed, twentyMinutes, DONE_AT);

        final CompanyReportFigures figures = repository.loadFigures(company, PERIOD);

        assertEquals(2, figures.activeEmployees());
        assertEquals(2, figures.enrollments());
        assertEquals(1, figures.completedEnrollments());
        assertEquals(30, figures.completedMinutes());
        assertEquals(Set.of(new JobTitleFigures("RECEPCAO", 1, 1), new JobTitleFigures("GERENCIA", 1, 0)),
                Set.copyOf(figures.byJobTitle()));
    }

    @Test
    void ignoresRevokedEnrollmentsRevokedCompanyCoursesAndCoursesTheCompanyDoesNotHave() {
        final UUID company = fixture.company("Alpha");
        final UUID valid = fixture.course("Valid");
        final UUID revokedEnrollment = fixture.course("Revoked enrollment");
        final UUID notOwned = fixture.course("Not owned");
        final UUID revokedByCompany = fixture.course("Revoked by company");
        fixture.ownCourse(company, valid);
        fixture.ownCourse(company, revokedEnrollment);
        fixture.ownCourse(company, revokedByCompany);
        fixture.revokeCompanyCourse(company, revokedByCompany, DONE_AT);
        final UUID ana = fixture.employee(company, "Ana", "ACTIVE", "RECEPCAO");
        for (final UUID course : List.of(valid, revokedEnrollment, notOwned, revokedByCompany)) {
            fixture.enrollment(ana, course, START, 10, null, null);
            fixture.completed(ana, fixture.lesson(course, 10), DONE_AT);
        }
        fixture.revokeEnrollment(ana, revokedEnrollment, DONE_AT);

        final CompanyReportFigures figures = repository.loadFigures(company, PERIOD);

        assertEquals(1, figures.enrollments());
        assertEquals(0, figures.completedEnrollments());
        assertEquals(10, figures.completedMinutes());
    }

    @Test
    void pendingAndInactiveEmployeesCountEnrollmentsButNotHoursOrEngagement() {
        final UUID company = fixture.company("Alpha");
        final UUID course = fixture.course("Course");
        fixture.ownCourse(company, course);
        final UUID tenMinutes = fixture.lesson(course, 10);
        final UUID twentyMinutes = fixture.lesson(course, 20);
        final UUID active = fixture.employee(company, "Ativa", "ACTIVE", "RECEPCAO");
        final UUID pending = fixture.employee(company, "Pendente", "PENDING", "RECEPCAO");
        final UUID inactive = fixture.employee(company, "Inativo", "INACTIVE", "RECEPCAO");
        final LocalDateTime recent = PERIOD.weekStart().plusDays(1);
        fixture.enrollment(active, course, START, 10, null, LocalDateTime.of(2026, 9, 1, 10, 0));
        fixture.enrollment(pending, course, START, 0, null, recent);
        fixture.enrollment(inactive, course, START, 10, null, recent);
        fixture.completed(active, tenMinutes, DONE_AT);
        fixture.completed(inactive, twentyMinutes, DONE_AT);

        final CompanyReportFigures figures = repository.loadFigures(company, PERIOD);

        assertEquals(1, figures.activeEmployees());
        assertEquals(3, figures.enrollments());
        assertEquals(10, figures.completedMinutes());
        assertEquals(0, figures.activeLastWeek());
    }

    @Test
    void enrollmentIsCompletedWithAConclusionDateOrFullProgressButNotAtNinetyNinePercent() {
        final UUID company = fixture.company("Alpha");
        final UUID course = fixture.course("Course");
        fixture.ownCourse(company, course);
        final UUID byProgress = fixture.employee(company, "A", "ACTIVE", "RECEPCAO");
        final UUID byDate = fixture.employee(company, "B", "ACTIVE", "RECEPCAO");
        final UUID almost = fixture.employee(company, "C", "ACTIVE", "RECEPCAO");
        final UUID noProgress = fixture.employee(company, "D", "ACTIVE", "RECEPCAO");
        fixture.enrollment(byProgress, course, START, 100, null, null);
        fixture.enrollment(byDate, course, START, 50, LocalDate.of(2026, 9, 1), null);
        fixture.enrollment(almost, course, START, 99, null, null);
        fixture.enrollment(noProgress, course, START, null, null, null);

        final CompanyReportFigures figures = repository.loadFigures(company, PERIOD);

        assertEquals(4, figures.enrollments());
        assertEquals(2, figures.completedEnrollments());
    }
}
