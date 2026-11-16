package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.AdminMetricsOverview;
import com.escapa.backend.application.model.CompanyEngagement;
import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import com.escapa.backend.application.port.AdminMetricsRepositoryPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetAdminMetricsOverviewUseCaseTest {
    private final FakeRepository repository = new FakeRepository();
    private final GetAdminMetricsOverviewUseCase useCase = new GetAdminMetricsOverviewUseCase(repository,
            Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void shouldReturnSixCalendarMonthsAndAllIndicators() {
        repository.active = 7;
        repository.previousActive = 5;
        repository.companies = 3;
        repository.previousCompanies = 2;
        repository.engaged = List.of(new CompanyEngagement(
                UUID.fromString("c0000000-0000-4000-8000-000000000001"), "Euro Trip", 4));
        repository.completion = new EnrollmentCompletionCounts(3, 2);
        repository.months.put(LocalDate.of(2026, 6, 1), 2L);
        repository.months.put(LocalDate.of(2026, 11, 1), 7L);

        final AdminMetricsOverview result = useCase.execute();

        assertEquals(7, result.activeStudents());
        assertEquals(3, result.partnerCompanies());
        assertEquals("66.67", result.averageCompletionRate().toPlainString());
        assertEquals(6, result.activeStudentsByMonth().size());
        assertEquals("2026-06", result.activeStudentsByMonth().get(0).month().toString());
        assertEquals(2, result.activeStudentsByMonth().get(0).activeStudents());
        assertEquals("2026-11", result.activeStudentsByMonth().get(5).month().toString());
        assertEquals(7, result.activeStudentsByMonth().get(5).activeStudents());
        assertEquals(0, result.activeStudentsByMonth().get(2).activeStudents());
        assertEquals(LocalDate.of(2026, 6, 30), repository.lastEndByStart.get(LocalDate.of(2026, 6, 1)));
        assertEquals(LocalDate.of(2026, 11, 30), repository.lastEndByStart.get(LocalDate.of(2026, 11, 1)));
        assertEquals(LocalDate.of(2026, 11, 5), repository.requestedToday);
        assertEquals(LocalDate.of(2026, 10, 31), repository.requestedPrevious);
        assertEquals(LocalDate.of(2026, 11, 1), repository.requestedCompanyStart);
        assertEquals(5, repository.requestedCompanyLimit);
        assertEquals(new BigDecimal("40.0"), result.activeStudentsChange());
        assertEquals(new BigDecimal("50.0"), result.partnerCompaniesChange());
        assertEquals(repository.engaged, result.topEngagedCompanies());
    }

    @Test
    void shouldReturnZerosWhenNoEligibleDataExists() {
        final AdminMetricsOverview result = useCase.execute();

        assertEquals(0, result.activeStudents());
        assertEquals(0, result.partnerCompanies());
        assertEquals("0.00", result.averageCompletionRate().toPlainString());
        assertEquals(new BigDecimal("0.0"), result.activeStudentsChange());
        assertEquals(new BigDecimal("0.0"), result.partnerCompaniesChange());
        assertEquals(List.of(), result.topEngagedCompanies());
        assertEquals(6, result.activeStudentsByMonth().size());
        result.activeStudentsByMonth().forEach(month -> assertEquals(0, month.activeStudents()));
    }

    @Test
    void shouldTreatMissingPreviousBaselineAsZeroChange() {
        repository.active = 4;
        repository.companies = 2;

        final AdminMetricsOverview result = useCase.execute();

        assertEquals(new BigDecimal("0.0"), result.activeStudentsChange());
        assertEquals(new BigDecimal("0.0"), result.partnerCompaniesChange());
    }

    @Test
    void shouldReturnNegativeChangeRoundedToOneDecimal() {
        repository.active = 7;
        repository.previousActive = 10;
        repository.companies = 5;
        repository.previousCompanies = 3;

        final AdminMetricsOverview result = useCase.execute();

        assertEquals(new BigDecimal("-30.0"), result.activeStudentsChange());
        assertEquals(new BigDecimal("66.7"), result.partnerCompaniesChange());
    }

    @Test
    void shouldKeepOctoberAtUtcNovemberBoundaryInSaoPaulo() {
        final Clock saoPauloClock = Clock.fixed(
                Instant.parse("2026-11-01T00:30:00Z"), ZoneId.of("America/Sao_Paulo"));
        final AdminMetricsOverview result = new GetAdminMetricsOverviewUseCase(repository, saoPauloClock).execute();

        assertEquals(LocalDate.of(2026, 10, 31), repository.requestedToday);
        assertEquals("2026-05", result.activeStudentsByMonth().get(0).month().toString());
        assertEquals("2026-10", result.activeStudentsByMonth().get(5).month().toString());
    }

    private static final class FakeRepository implements AdminMetricsRepositoryPort {
        private long active;
        private long previousActive;
        private long companies;
        private long previousCompanies;
        private List<CompanyEngagement> engaged = List.of();
        private EnrollmentCompletionCounts completion = new EnrollmentCompletionCounts(0, 0);
        private LocalDate requestedToday;
        private LocalDate requestedPrevious;
        private LocalDate requestedCompanyStart;
        private int requestedCompanyLimit;
        private final Map<LocalDate, Long> months = new HashMap<>();
        private final Map<LocalDate, LocalDate> lastEndByStart = new HashMap<>();

        @Override
        public long countActiveStudents(LocalDate date) {
            if (requestedToday == null) {
                requestedToday = date;
                return active;
            }
            requestedPrevious = date;
            return previousActive;
        }

        @Override
        public long countPartnerCompanies() {
            return companies;
        }

        @Override
        public long countPartnerCompaniesCreatedBefore(LocalDate startOfCurrentMonth) {
            requestedCompanyStart = startOfCurrentMonth;
            return previousCompanies;
        }

        @Override
        public EnrollmentCompletionCounts countHistoricalCompletion() {
            return completion;
        }

        @Override
        public long countActiveStudentsInPeriod(LocalDate from, LocalDate to) {
            lastEndByStart.put(from, to);
            return months.getOrDefault(from, 0L);
        }

        @Override
        public List<CompanyEngagement> findTopEngagedCompanies(int limit) {
            requestedCompanyLimit = limit;
            return engaged;
        }
    }
}
