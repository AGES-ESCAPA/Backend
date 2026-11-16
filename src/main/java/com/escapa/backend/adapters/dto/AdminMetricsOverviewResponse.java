package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.AdminMetricsOverview;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AdminMetricsOverviewResponse(long activeStudents, long partnerCompanies, BigDecimal averageCompletionRate,
        List<MonthlyActiveStudentsResponse> activeStudentsByMonth, BigDecimal activeStudentsChange,
        BigDecimal partnerCompaniesChange, List<EngagedCompanyResponse> topEngagedCompanies) {
    public static AdminMetricsOverviewResponse from(AdminMetricsOverview overview) {
        return new AdminMetricsOverviewResponse(overview.activeStudents(), overview.partnerCompanies(),
                overview.averageCompletionRate(), overview.activeStudentsByMonth().stream()
                        .map(month -> new MonthlyActiveStudentsResponse(
                                month.month().toString(), month.activeStudents())).toList(),
                overview.activeStudentsChange(), overview.partnerCompaniesChange(),
                overview.topEngagedCompanies().stream()
                        .map(company -> new EngagedCompanyResponse(
                                company.companyId(), company.name(), company.enrollments()))
                        .toList());
    }

    public record MonthlyActiveStudentsResponse(String month, long activeStudents) {
    }

    public record EngagedCompanyResponse(UUID companyId, String name, long enrollments) {
    }
}
