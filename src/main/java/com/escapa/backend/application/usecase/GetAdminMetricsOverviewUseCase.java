package com.escapa.backend.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.escapa.backend.application.model.AdminMetricsOverview;
import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import com.escapa.backend.application.port.AdminMetricsRepositoryPort;

public class GetAdminMetricsOverviewUseCase {
    private static final int MONTHS_IN_SERIES = 6;
    private static final int TOP_ENGAGED_COMPANIES = 5;
    private final AdminMetricsRepositoryPort repository;
    private final Clock clock;

    public GetAdminMetricsOverviewUseCase(AdminMetricsRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public AdminMetricsOverview execute() {
        final LocalDate today = LocalDate.now(clock);
        final YearMonth currentMonth = YearMonth.from(today);
        final long activeStudents = repository.countActiveStudents(today);
        final long previousActiveStudents = repository.countActiveStudents(
                currentMonth.minusMonths(1).atEndOfMonth());
        final long partnerCompanies = repository.countPartnerCompanies();
        final long previousPartnerCompanies = repository.countPartnerCompaniesCreatedBefore(currentMonth.atDay(1));
        final EnrollmentCompletionCounts completion = repository.countHistoricalCompletion();
        final BigDecimal rate = completion.eligible() == 0
                ? new BigDecimal("0.00")
                : BigDecimal.valueOf(completion.completed()).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(completion.eligible()), 2, RoundingMode.HALF_UP);
        final List<AdminMetricsOverview.MonthlyActiveStudents> months = new ArrayList<>();
        final YearMonth first = YearMonth.from(today).minusMonths(MONTHS_IN_SERIES - 1);
        for (int index = 0; index < MONTHS_IN_SERIES; index++) {
            final YearMonth month = first.plusMonths(index);
            months.add(new AdminMetricsOverview.MonthlyActiveStudents(month,
                    repository.countActiveStudentsInPeriod(month.atDay(1), month.atEndOfMonth())));
        }
        return new AdminMetricsOverview(activeStudents, partnerCompanies, rate, List.copyOf(months),
                percentageChange(activeStudents, previousActiveStudents),
                percentageChange(partnerCompanies, previousPartnerCompanies),
                List.copyOf(repository.findTopEngagedCompanies(TOP_ENGAGED_COMPANIES)));
    }

    /** Sem base no mês anterior a variação fica em 0, como no relatório da empresa. */
    private static BigDecimal percentageChange(long current, long previous) {
        if (previous == 0) {
            return new BigDecimal("0.0");
        }
        return BigDecimal.valueOf(current).subtract(BigDecimal.valueOf(previous))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP);
    }
}
