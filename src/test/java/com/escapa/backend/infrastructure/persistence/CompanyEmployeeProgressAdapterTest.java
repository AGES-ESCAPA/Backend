package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployeeProgress;
import com.escapa.backend.application.model.EmployeeProgressStatus;
import com.escapa.backend.application.port.CompanyReportRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CompanyEmployeeProgressAdapterTest extends PostgresIntegrationTest {

    private static final LocalDate START = LocalDate.of(2026, 3, 1);
    private static final int PAGE_SIZE = 20;

    @Autowired
    private CompanyReportRepositoryPort repository;
    @Autowired
    private JdbcTemplate jdbc;

    private CompanyReportFixture fixture;
    private UUID company;
    private UUID ai;
    private UUID guest;

    @BeforeEach
    void setUp() {
        fixture = new CompanyReportFixture(jdbc);
        company = fixture.company("Alpha");
        ai = fixture.course("IA");
        guest = fixture.course("Hospede");
        fixture.ownCourse(company, ai);
        fixture.ownCourse(company, guest);
    }

    private UUID employee(String name) {
        return fixture.employee(company, name, "ACTIVE", "RECEPCAO");
    }

    private List<String> labels(PageResult<CompanyEmployeeProgress> page) {
        return page.content().stream()
                .map(row -> row.name() + "|" + row.courseTitle() + "|" + row.progress())
                .toList();
    }

    private PageResult<CompanyEmployeeProgress> list(String search, EmployeeProgressStatus status) {
        return repository.findEmployeeProgress(company, search, status, 0, PAGE_SIZE);
    }

    @Test
    void listsOneRowPerEmployeeAndCourseByMostRecentAccessWithNeverAccessedLastThenByName() {
        final UUID ana = employee("Ana");
        final UUID bruno = employee("Bruno");
        final UUID carla = employee("Carla");
        final UUID denise = employee("Denise");
        fixture.enrollment(ana, ai, START, 40, null, LocalDateTime.of(2026, 11, 5, 10, 0));
        fixture.enrollment(ana, guest, START, 40, LocalDate.of(2026, 11, 7), LocalDateTime.of(2026, 11, 7, 10, 0));
        fixture.certificate(ana, guest, LocalDateTime.of(2026, 11, 7, 11, 0));
        fixture.enrollment(bruno, ai, START, 0, null, null);
        fixture.enrollment(carla, ai, START, 10, null, LocalDateTime.of(2026, 10, 1, 9, 0));
        fixture.enrollment(denise, ai, START, null, null, null);

        final PageResult<CompanyEmployeeProgress> page = list(null, null);

        assertEquals(List.of("Ana|Hospede|100", "Ana|IA|40", "Carla|IA|10", "Bruno|IA|0", "Denise|IA|0"),
                labels(page));
        assertEquals(5, page.totalElements());
        final CompanyEmployeeProgress first = page.content().get(0);
        assertEquals(ana, first.userId());
        assertEquals(guest, first.courseId());
        assertEquals(LocalDateTime.of(2026, 11, 7, 10, 0), first.lastAccessAt());
        assertTrue(first.certificateIssued());
        assertNull(first.avatarUrl());
        assertNull(page.content().get(3).lastAccessAt());
        assertFalse(page.content().get(1).certificateIssued());
    }

    @Test
    void neverListsEmployeesOfAnotherCompany() {
        final UUID other = fixture.company("Beta");
        fixture.ownCourse(other, ai);
        final UUID ana = employee("Ana");
        final UUID zed = fixture.employee(other, "Zed", "ACTIVE", "RECEPCAO");
        fixture.enrollment(ana, ai, START, 10, null, null);
        fixture.enrollment(zed, ai, START, 10, null, null);

        assertEquals(List.of("Ana|IA|10"), labels(list(null, null)));
    }

    @Test
    void leavesOutRevokedEnrollmentsAndCoursesTheCompanyNoLongerHas() {
        final UUID notOwned = fixture.course("Outro");
        final UUID revokedByCompany = fixture.course("Cancelado");
        fixture.ownCourse(company, revokedByCompany);
        fixture.revokeCompanyCourse(company, revokedByCompany, LocalDateTime.of(2026, 9, 1, 10, 0));
        final UUID ana = employee("Ana");
        fixture.enrollment(ana, ai, START, 10, null, null);
        fixture.enrollment(ana, guest, START, 10, null, null);
        fixture.revokeEnrollment(ana, guest, LocalDateTime.of(2026, 9, 1, 10, 0));
        fixture.enrollment(ana, notOwned, START, 10, null, null);
        fixture.enrollment(ana, revokedByCompany, START, 10, null, null);

        assertEquals(List.of("Ana|IA|10"), labels(list(null, null)));
    }

    @Test
    void searchesByNameIgnoringCaseAndTreatsPercentAndUnderscoreAsText() {
        final UUID underscore = employee("Ana_Maria");
        final UUID letter = employee("AnaXMaria");
        final UUID percent = employee("100% Fulano");
        for (final UUID employee : List.of(underscore, letter, percent)) {
            fixture.enrollment(employee, ai, START, 10, null, null);
        }

        assertEquals(List.of("Ana_Maria|IA|10", "AnaXMaria|IA|10"), labels(list("ANA", null)));
        assertEquals(List.of("Ana_Maria|IA|10"), labels(list("ana_m", null)));
        assertEquals(List.of("100% Fulano|IA|10"), labels(list("%", null)));
        assertEquals(List.of(), labels(list("inexistente", null)));
    }

    @Test
    void filtersByStatusUsingTheSameCompletionRuleAsTheReport() {
        final UUID byDate = employee("Por data");
        final UUID byProgress = employee("Por progresso");
        final UUID almost = employee("Quase");
        final UUID noProgress = employee("Sem progresso");
        fixture.enrollment(byDate, ai, START, 50, LocalDate.of(2026, 9, 1), null);
        fixture.enrollment(byProgress, ai, START, 100, null, null);
        fixture.enrollment(almost, ai, START, 99, null, null);
        fixture.enrollment(noProgress, ai, START, null, null, null);

        assertEquals(List.of("Por data|IA|100", "Por progresso|IA|100"),
                labels(list(null, EmployeeProgressStatus.COMPLETED)).stream().sorted().toList());
        assertEquals(List.of("Quase|IA|99", "Sem progresso|IA|0"),
                labels(list(null, EmployeeProgressStatus.IN_PROGRESS)).stream().sorted().toList());
        assertEquals(List.of("Por progresso|IA|100"),
                labels(list("progresso", EmployeeProgressStatus.COMPLETED)));
    }

    @Test
    void paginatesWithAStableOrder() {
        for (final String name : List.of("Ana", "Bia", "Cris")) {
            fixture.enrollment(employee(name), ai, START, 10, null, null);
        }

        final PageResult<CompanyEmployeeProgress> first = repository.findEmployeeProgress(company, null, null, 0, 2);
        final PageResult<CompanyEmployeeProgress> second = repository.findEmployeeProgress(company, null, null, 1, 2);
        final PageResult<CompanyEmployeeProgress> beyond = repository.findEmployeeProgress(company, null, null, 5, 2);

        assertEquals(List.of("Ana|IA|10", "Bia|IA|10"), labels(first));
        assertEquals(List.of("Cris|IA|10"), labels(second));
        assertEquals(3, first.totalElements());
        assertEquals(2, first.totalPages());
        assertEquals(1, second.pageNumber());
        assertEquals(List.of(), labels(beyond));
    }
}
