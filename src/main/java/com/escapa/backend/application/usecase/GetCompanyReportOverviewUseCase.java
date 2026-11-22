package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.escapa.backend.application.model.CompanyReportFigures;
import com.escapa.backend.application.model.CompanyReportFigures.JobTitleFigures;
import com.escapa.backend.application.model.CompanyReportFigures.MonthFigures;
import com.escapa.backend.application.model.CompanyReportOverview;
import com.escapa.backend.application.model.CompanyReportOverview.JobTitleCompletion;
import com.escapa.backend.application.model.CompanyReportOverview.MonthlyHours;
import com.escapa.backend.application.model.CompanyReportPeriod;
import com.escapa.backend.application.port.CompanyReportRepositoryPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;
import com.escapa.backend.domain.company.JobTitleNormalizer;

/**
 * Indicadores de conclusão e engajamento da equipe da empresa (US-35). O banco só conta; as taxas,
 * médias, o arredondamento e a série fixa de 6 meses são calculados aqui. A empresa é sempre a do token.
 * Só lê, então o prefixo {@code Get} (transação somente leitura) está correto.
 */
public class GetCompanyReportOverviewUseCase {

    private static final double PERCENT = 100.0;
    private static final double MINUTES_PER_HOUR = 60.0;
    private static final double ROUNDING_FACTOR = 10.0;
    private static final int WEEK_DAYS = 7;
    private static final int MONTHS_IN_CHART = 6;

    private final CompanyReportRepositoryPort companyReportRepositoryPort;
    private final Clock clock;

    public GetCompanyReportOverviewUseCase(CompanyReportRepositoryPort companyReportRepositoryPort, Clock clock) {
        this.companyReportRepositoryPort = companyReportRepositoryPort;
        this.clock = clock;
    }

    /**
     * @throws CompanyNotFoundException se o usuário COMPANY do token não tem registro em {@code company}
     */
    public CompanyReportOverview execute(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId cannot be null");
        }
        if (!companyReportRepositoryPort.existsCompany(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }

        final CompanyReportPeriod period = periodOf(LocalDateTime.now(clock));
        final CompanyReportFigures figures = companyReportRepositoryPort.loadFigures(companyId, period);

        return new CompanyReportOverview(
                rate(figures.completedEnrollments(), figures.enrollments()),
                completionRateChange(figures),
                averageHours(figures.completedMinutes(), figures.activeEmployees()),
                figures.certificatesThisQuarter(),
                rate(figures.activeLastWeek(), figures.activeEmployees()),
                byJobTitle(figures),
                monthlyHours(figures, period));
    }

    private static CompanyReportPeriod periodOf(LocalDateTime now) {
        final LocalDate today = now.toLocalDate();
        return new CompanyReportPeriod(
                now.minusDays(WEEK_DAYS),
                today.with(IsoFields.DAY_OF_QUARTER, 1).atStartOfDay(),
                today.withDayOfMonth(1).minusDays(1),
                YearMonth.from(today).minusMonths(MONTHS_IN_CHART - 1).atDay(1).atStartOfDay());
    }

    private static double completionRateChange(CompanyReportFigures figures) {
        if (figures.enrollmentsAtPreviousMonthEnd() == 0) {
            return 0.0;
        }
        final double current = rate(figures.completedEnrollments(), figures.enrollments());
        final double previous = rate(figures.completedAtPreviousMonthEnd(), figures.enrollmentsAtPreviousMonthEnd());
        return round(current - previous);
    }

    private static List<JobTitleCompletion> byJobTitle(CompanyReportFigures figures) {
        final Map<String, MergedJobTitle> merged = new LinkedHashMap<>();
        for (final JobTitleFigures row : figures.byJobTitle()) {
            final String groupKey = JobTitleNormalizer.key(row.jobTitle());
            final MergedJobTitle current = merged.get(groupKey);
            if (current == null) {
                merged.put(groupKey, new MergedJobTitle(row.jobTitle(), row.enrollments(), row.completed()));
                continue;
            }
            current.label = JobTitleNormalizer.preferLabel(current.label, row.jobTitle());
            current.enrollments += row.enrollments();
            current.completed += row.completed();
        }
        final List<MergedJobTitle> titles = new ArrayList<>(merged.values());
        titles.sort(Comparator.comparing(title -> title.label, String.CASE_INSENSITIVE_ORDER));
        return titles.stream()
                .map(title -> new JobTitleCompletion(title.label, rate(title.completed, title.enrollments)))
                .toList();
    }

    private static final class MergedJobTitle {
        private String label;
        private long enrollments;
        private long completed;

        private MergedJobTitle(String label, long enrollments, long completed) {
            this.label = label;
            this.enrollments = enrollments;
            this.completed = completed;
        }
    }

    private static List<MonthlyHours> monthlyHours(CompanyReportFigures figures, CompanyReportPeriod period) {
        final Map<YearMonth, Long> minutesByMonth = figures.monthlyMinutes().stream()
                .collect(Collectors.toMap(MonthFigures::month, MonthFigures::minutes, Long::sum));
        final YearMonth first = YearMonth.from(period.monthlyFrom());

        return IntStream.range(0, MONTHS_IN_CHART)
                .mapToObj(first::plusMonths)
                .map(month -> new MonthlyHours(
                        month, averageHours(minutesByMonth.getOrDefault(month, 0L), figures.activeEmployees())))
                .toList();
    }

    private static double averageHours(long minutes, long employees) {
        return employees == 0 ? 0.0 : round(minutes / MINUTES_PER_HOUR / employees);
    }

    private static double rate(long part, long total) {
        return total == 0 ? 0.0 : round(part * PERCENT / total);
    }

    private static double round(double value) {
        return Math.round(value * ROUNDING_FACTOR) / ROUNDING_FACTOR;
    }
}
