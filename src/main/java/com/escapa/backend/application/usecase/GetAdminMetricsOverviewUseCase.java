package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.AdminMetricsOverview;
import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import com.escapa.backend.application.port.AdminMetricsRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class GetAdminMetricsOverviewUseCase {
    private static final int MONTHS_IN_SERIES = 6;
    private final AdminMetricsRepositoryPort repository;
    private final Clock clock;

    public GetAdminMetricsOverviewUseCase(AdminMetricsRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public AdminMetricsOverview execute() {
        final LocalDate today = LocalDate.now(clock);
        final long activeStudents = repository.countActiveStudents(today);
        final long partnerCompanies = repository.countPartnerCompanies();
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
        return new AdminMetricsOverview(activeStudents, partnerCompanies, rate, List.copyOf(months));
    }
}
