package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CompanyReportFigures;
import com.escapa.backend.application.model.CompanyReportFigures.MonthFigures;
import com.escapa.backend.application.model.CompanyReportPeriod;
import com.escapa.backend.application.port.CompanyReportRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class CompanyReportAdapterPeriodTest extends PostgresIntegrationTest {

    private static final CompanyReportPeriod PERIOD = new CompanyReportPeriod(
            LocalDateTime.of(2026, 11, 2, 18, 0),
            LocalDateTime.of(2026, 10, 1, 0, 0),
            LocalDate.of(2026, 10, 31),
            LocalDateTime.of(2026, 6, 1, 0, 0));
    private static final LocalDate START = LocalDate.of(2026, 3, 1);

    @Autowired
    private CompanyReportRepositoryPort repository;
    @Autowired
    private JdbcTemplate jdbc;

    private CompanyReportFixture fixture;
    private UUID company;
    private UUID course;

    @BeforeEach
    void setUp() {
        fixture = new CompanyReportFixture(jdbc);
        company = fixture.company("Alpha");
        course = fixture.course("Course");
        fixture.ownCourse(company, course);
    }

    private UUID activeEmployee(String name) {
        return fixture.employee(company, name, "ACTIVE", "RECEPCAO");
    }

    @Test
    void weeklyEngagementStartsExactlyAtTheWindowAndCountsEachEmployeeOnce() {
        final UUID secondCourse = fixture.course("Second course");
        fixture.ownCourse(company, secondCourse);
        final UUID onBoundary = activeEmployee("Na borda");
        final UUID justBefore = activeEmployee("Um segundo antes");
        final UUID twoCourses = activeEmployee("Dois cursos");
        final UUID never = activeEmployee("Nunca");
        fixture.enrollment(onBoundary, course, START, 10, null, PERIOD.weekStart());
        fixture.enrollment(justBefore, course, START, 10, null, PERIOD.weekStart().minusSeconds(1));
        fixture.enrollment(twoCourses, course, START, 10, null, PERIOD.weekStart().plusDays(1));
        fixture.enrollment(twoCourses, secondCourse, START, 10, null, PERIOD.weekStart().plusDays(2));
        fixture.enrollment(never, course, START, 0, null, null);

        final CompanyReportFigures figures = repository.loadFigures(company, PERIOD);

        assertEquals(4, figures.activeEmployees());
        assertEquals(2, figures.activeLastWeek());
    }

    @Test
    void certificatesOfTheQuarterStartExactlyAtTheQuarterStart() {
        final UUID onBoundary = activeEmployee("Na borda");
        final UUID before = activeEmployee("Antes");
        final UUID later = activeEmployee("Depois");
        final UUID notIssued = activeEmployee("Sem certificado");
        fixture.enrollment(onBoundary, course, START, 100, LocalDate.of(2026, 10, 1), null);
        fixture.enrollment(before, course, START, 100, LocalDate.of(2026, 9, 30), null);
        fixture.enrollment(later, course, START, 100, LocalDate.of(2026, 11, 5), null);
        fixture.enrollment(notIssued, course, START, 100, LocalDate.of(2026, 11, 5), null);
        fixture.certificate(onBoundary, course, PERIOD.quarterStart());
        fixture.certificate(before, course, PERIOD.quarterStart().minusSeconds(1));
        fixture.certificate(later, course, LocalDateTime.of(2026, 11, 5, 9, 0));

        assertEquals(2, repository.loadFigures(company, PERIOD).certificatesThisQuarter());
    }

    @Test
    void previousMonthEndKeepsEnrollmentsStartedByThatDayAndConclusionsUpToIt() {
        final UUID startedOnLastDay = activeEmployee("Comecou no ultimo dia");
        final UUID startedNextDay = activeEmployee("Comecou no dia seguinte");
        final UUID noStartDate = activeEmployee("Sem data de inicio");
        fixture.enrollment(startedOnLastDay, course, LocalDate.of(2026, 10, 31), 100, LocalDate.of(2026, 10, 31), null);
        fixture.enrollment(startedNextDay, course, LocalDate.of(2026, 11, 1), 0, null, null);
        fixture.enrollment(noStartDate, course, null, 100, LocalDate.of(2026, 11, 1), null);

        final CompanyReportFigures figures = repository.loadFigures(company, PERIOD);

        assertEquals(3, figures.enrollments());
        assertEquals(2, figures.completedEnrollments());
        assertEquals(2, figures.enrollmentsAtPreviousMonthEnd());
        assertEquals(1, figures.completedAtPreviousMonthEnd());
    }

    @Test
    void previousMonthEndUsesTheSameCompletionRuleWithFullProgressAndNoConclusionDate() {
        final UUID fullBefore = activeEmployee("Cem por cento antes");
        final UUID fullAfter = activeEmployee("Cem por cento depois");
        final UUID almost = activeEmployee("Noventa e nove");
        fixture.enrollment(fullBefore, course, LocalDate.of(2026, 10, 10), 100, null, null);
        fixture.enrollment(fullAfter, course, LocalDate.of(2026, 11, 1), 100, null, null);
        fixture.enrollment(almost, course, LocalDate.of(2026, 10, 10), 99, null, null);

        final CompanyReportFigures figures = repository.loadFigures(company, PERIOD);

        assertEquals(2, figures.completedEnrollments());
        assertEquals(2, figures.enrollmentsAtPreviousMonthEnd());
        assertEquals(1, figures.completedAtPreviousMonthEnd());
    }

    @Test
    void monthlyMinutesStartAtTheFirstMonthSumTheMonthAndLeaveEarlierLessonsOutOfTheSeries() {
        final UUID ana = activeEmployee("Ana");
        final UUID bia = activeEmployee("Bia");
        fixture.enrollment(ana, course, START, 50, null, null);
        fixture.enrollment(bia, course, START, 50, null, null);
        final UUID fiveMinutes = fixture.lesson(course, 5);
        final UUID tenMinutes = fixture.lesson(course, 10);
        final UUID twentyMinutes = fixture.lesson(course, 20);
        fixture.completed(ana, fiveMinutes, LocalDateTime.of(2026, 5, 31, 23, 59, 59));
        fixture.completed(ana, tenMinutes, LocalDateTime.of(2026, 6, 1, 0, 0));
        fixture.completed(ana, twentyMinutes, LocalDateTime.of(2026, 6, 30, 23, 59));
        fixture.completed(bia, tenMinutes, LocalDateTime.of(2026, 7, 5, 8, 0));

        final CompanyReportFigures figures = repository.loadFigures(company, PERIOD);

        assertEquals(Set.of(new MonthFigures(YearMonth.of(2026, 6), 30), new MonthFigures(YearMonth.of(2026, 7), 10)),
                Set.copyOf(figures.monthlyMinutes()));
        assertEquals(45, figures.completedMinutes());
    }
}
