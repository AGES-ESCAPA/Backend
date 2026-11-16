package com.escapa.backend.application.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record AdminMetricsOverview(long activeStudents, long partnerCompanies, BigDecimal averageCompletionRate,
        List<MonthlyActiveStudents> activeStudentsByMonth, BigDecimal activeStudentsChange,
        BigDecimal partnerCompaniesChange, List<CompanyEngagement> topEngagedCompanies) {
    public record MonthlyActiveStudents(YearMonth month, long activeStudents) {
    }
}
