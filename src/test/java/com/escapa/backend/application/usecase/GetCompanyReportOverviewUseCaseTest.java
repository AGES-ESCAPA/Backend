package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployeeProgress;
import com.escapa.backend.application.model.CompanyReportFigures;
import com.escapa.backend.application.model.CompanyReportFigures.JobTitleFigures;
import com.escapa.backend.application.model.CompanyReportFigures.MonthFigures;
import com.escapa.backend.application.model.CompanyReportOverview;
import com.escapa.backend.application.model.CompanyReportPeriod;
import com.escapa.backend.application.model.EmployeeProgressStatus;
import com.escapa.backend.application.port.CompanyReportRepositoryPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetCompanyReportOverviewUseCaseTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final UUID COMPANY_ID = UUID.fromString("d0000000-0000-4000-d000-000000000001");
    private static final double DELTA = 0.0001;

    private final FakePort port = new FakePort();

    private static Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), SAO_PAULO);
    }

    /** 2026-11-09 18:00 em São Paulo. */
    private GetCompanyReportOverviewUseCase useCase() {
        return new GetCompanyReportOverviewUseCase(port, clockAt("2026-11-09T21:00:00Z"));
    }

    private static CompanyReportFigures figures(long activeEmployees, long enrollments, long completed, long minutes) {
        return new CompanyReportFigures(activeEmployees, enrollments, completed, minutes, 0, 0, 0, 0, List.of(), List.of());
    }

    @Test
    void companyWithoutEmployeesReturnsZeroedIndicatorsWithoutDividingByZero() {
        port.figures = figures(0, 0, 0, 0);

        final CompanyReportOverview overview = useCase().execute(COMPANY_ID);

        assertEquals(0.0, overview.completionRate(), DELTA);
        assertEquals(0.0, overview.completionRateChange(), DELTA);
        assertEquals(0.0, overview.averageHoursPerEmployee(), DELTA);
        assertEquals(0L, overview.certificatesThisQuarter());
        assertEquals(0.0, overview.weeklyEngagement(), DELTA);
        assertEquals(List.of(), overview.completionByJobTitle());
        assertEquals(6, overview.monthlyHours().size());
        overview.monthlyHours().forEach(month -> assertEquals(0.0, month.hours(), DELTA));
    }

    @Test
    void completionRateIsCompletedOverAllEnrollmentsRoundedToOneDecimal() {
        port.figures = figures(4, 8, 3, 0);
        assertEquals(37.5, useCase().execute(COMPANY_ID).completionRate(), DELTA);

        port.figures = figures(4, 3, 1, 0);
        assertEquals(33.3, useCase().execute(COMPANY_ID).completionRate(), DELTA);
    }

    @Test
    void averageHoursIsCompletedMinutesPerActiveEmployee() {
        port.figures = figures(4, 8, 3, 600);

        assertEquals(2.5, useCase().execute(COMPANY_ID).averageHoursPerEmployee(), DELTA);
    }

    @Test
    void weeklyEngagementIsShareOfActiveEmployeesWithRecentAccess() {
        port.figures = new CompanyReportFigures(6, 12, 0, 0, 0, 3, 0, 0, List.of(), List.of());

        assertEquals(50.0, useCase().execute(COMPANY_ID).weeklyEngagement(), DELTA);
    }

    @Test
    void certificatesThisQuarterIsPassedThrough() {
        port.figures = new CompanyReportFigures(6, 12, 0, 0, 5, 0, 0, 0, List.of(), List.of());

        assertEquals(5L, useCase().execute(COMPANY_ID).certificatesThisQuarter());
    }

    @Test
    void completionRateChangeIsPercentagePointsSincePreviousMonthEnd() {
        port.figures = new CompanyReportFigures(5, 10, 5, 0, 0, 0, 8, 2, List.of(), List.of());

        // agora 50,0% e, no fim do mês anterior, 2 de 8 = 25,0%
        assertEquals(25.0, useCase().execute(COMPANY_ID).completionRateChange(), DELTA);
    }

    @Test
    void completionRateChangeIsNegativeWhenTheRateDropped() {
        port.figures = new CompanyReportFigures(5, 10, 2, 0, 0, 0, 4, 4, List.of(), List.of());

        // agora 20,0% e, no fim do mês anterior, 4 de 4 = 100,0%
        assertEquals(-80.0, useCase().execute(COMPANY_ID).completionRateChange(), DELTA);
    }

    @Test
    void completionRateChangeIsZeroWhenThereWereNoEnrollmentsToCompareWith() {
        port.figures = new CompanyReportFigures(5, 10, 5, 0, 0, 0, 0, 0, List.of(), List.of());

        assertEquals(0.0, useCase().execute(COMPANY_ID).completionRateChange(), DELTA);
    }

    @Test
    void completionByJobTitleIsSortedByTitleWithEachRate() {
        port.figures = new CompanyReportFigures(5, 10, 5, 0, 0, 0, 0, 0, List.of(
                new JobTitleFigures("RECEPCAO", 4, 1),
                new JobTitleFigures("ATENDIMENTO", 2, 2),
                new JobTitleFigures("GOVERNANCA", 3, 0)), List.of());

        final List<CompanyReportOverview.JobTitleCompletion> byTitle = useCase().execute(COMPANY_ID).completionByJobTitle();

        assertEquals(List.of("ATENDIMENTO", "GOVERNANCA", "RECEPCAO"),
                byTitle.stream().map(CompanyReportOverview.JobTitleCompletion::jobTitle).toList());
        assertEquals(List.of(100.0, 0.0, 25.0),
                byTitle.stream().map(CompanyReportOverview.JobTitleCompletion::completionRate).toList());
    }

    @Test
    void monthlyHoursAlwaysHasSixMonthsOldestFirstWithZerosWhereThereIsNoData() {
        port.figures = new CompanyReportFigures(2, 4, 0, 0, 0, 0, 0, 0, List.of(), List.of(
                new MonthFigures(YearMonth.of(2026, 9), 120),
                new MonthFigures(YearMonth.of(2026, 11), 60)));

        final List<CompanyReportOverview.MonthlyHours> months = useCase().execute(COMPANY_ID).monthlyHours();

        assertEquals(List.of(YearMonth.of(2026, 6), YearMonth.of(2026, 7), YearMonth.of(2026, 8),
                        YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11)),
                months.stream().map(CompanyReportOverview.MonthlyHours::month).toList());
        // 120 min / 60 / 2 colaboradores = 1,0 h; 60 min / 60 / 2 = 0,5 h
        assertEquals(List.of(0.0, 0.0, 0.0, 1.0, 0.0, 0.5),
                months.stream().map(CompanyReportOverview.MonthlyHours::hours).toList());
    }

    @Test
    void periodIsDerivedFromTheClock() {
        port.figures = figures(0, 0, 0, 0);

        useCase().execute(COMPANY_ID);

        assertEquals(new CompanyReportPeriod(
                LocalDateTime.of(2026, 11, 2, 18, 0),
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDate.of(2026, 10, 31),
                LocalDateTime.of(2026, 6, 1, 0, 0)), port.receivedPeriod);
    }

    @Test
    void periodCrossesTheYearBoundary() {
        port.figures = figures(0, 0, 0, 0);

        new GetCompanyReportOverviewUseCase(port, clockAt("2027-01-15T15:00:00Z")).execute(COMPANY_ID);

        assertEquals(new CompanyReportPeriod(
                LocalDateTime.of(2027, 1, 8, 12, 0),
                LocalDateTime.of(2027, 1, 1, 0, 0),
                LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 8, 1, 0, 0)), port.receivedPeriod);
    }

    @Test
    void periodFollowsSaoPauloAtTheMonthBoundaryEvenWhenUtcAlreadyChangedMonth() {
        port.figures = figures(0, 0, 0, 0);

        // 2026-11-01 02:00 UTC ainda é 31/10 às 23:00 em São Paulo
        new GetCompanyReportOverviewUseCase(port, clockAt("2026-11-01T02:00:00Z")).execute(COMPANY_ID);

        assertEquals(LocalDate.of(2026, 9, 30), port.receivedPeriod.previousMonthEnd());
        assertEquals(LocalDateTime.of(2026, 10, 1, 0, 0), port.receivedPeriod.quarterStart());
        assertEquals(LocalDateTime.of(2026, 5, 1, 0, 0), port.receivedPeriod.monthlyFrom());
    }

    @Test
    void unknownCompanyIsRejected() {
        port.companyExists = false;

        assertThrows(CompanyNotFoundException.class, () -> useCase().execute(COMPANY_ID));
    }

    @Test
    void nullCompanyIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> useCase().execute(null));
    }

    private static final class FakePort implements CompanyReportRepositoryPort {

        private boolean companyExists = true;
        private CompanyReportFigures figures;
        private CompanyReportPeriod receivedPeriod;

        @Override
        public boolean existsCompany(UUID companyId) {
            return companyExists;
        }

        @Override
        public CompanyReportFigures loadFigures(UUID companyId, CompanyReportPeriod period) {
            this.receivedPeriod = period;
            return figures;
        }

        @Override
        public PageResult<CompanyEmployeeProgress> findEmployeeProgress(
                UUID companyId, String search, EmployeeProgressStatus status, int page, int size) {
            throw new UnsupportedOperationException();
        }
    }
}
