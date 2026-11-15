package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.AdminMetricsOverview;
import java.math.BigDecimal;
import java.util.List;

public record AdminMetricsOverviewResponse(long activeStudents, long partnerCompanies, BigDecimal averageCompletionRate,
        List<MonthlyActiveStudentsResponse> activeStudentsByMonth) {
    public static AdminMetricsOverviewResponse from(AdminMetricsOverview overview) {
        return new AdminMetricsOverviewResponse(overview.activeStudents(), overview.partnerCompanies(),
                overview.averageCompletionRate(), overview.activeStudentsByMonth().stream()
                        .map(month -> new MonthlyActiveStudentsResponse(
                                month.month().toString(), month.activeStudents())).toList());
    }

    public record MonthlyActiveStudentsResponse(String month, long activeStudents) {
    }
}
