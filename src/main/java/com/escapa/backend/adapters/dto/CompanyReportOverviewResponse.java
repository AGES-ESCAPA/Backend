package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.CompanyReportOverview;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Completion and engagement indicators of the company team")
public record CompanyReportOverviewResponse(
        @Schema(description = "% of the enrollments that are completed, one decimal", example = "37.5")
        double completionRate,
        @Schema(description = "Change of the completion rate, in percentage points, since the end of the "
                + "previous month; 0 when there was nothing to compare with", example = "4.2")
        double completionRateChange,
        @Schema(description = "Hours of completed lessons per active employee", example = "2.5")
        double averageHoursPerEmployee,
        @Schema(description = "Certificates issued in the current quarter", example = "5")
        long certificatesThisQuarter,
        @Schema(description = "% of the active employees who accessed a course in the last 7 days", example = "50.0")
        double weeklyEngagement,
        @Schema(description = "Completion rate per job title, in alphabetical order")
        List<JobTitleCompletionResponse> completionByJobTitle,
        @Schema(description = "Always 6 months, oldest first; months without completed lessons are 0")
        List<MonthlyHoursResponse> monthlyHours
) {

    @Schema(description = "Completion rate of one job title")
    public record JobTitleCompletionResponse(
            @Schema(description = "Job title, kept in users_company.role", example = "RECEPCAO")
            String jobTitle,
            @Schema(example = "25.0")
            double completionRate
    ) {
    }

    @Schema(description = "Average completed hours per active employee in one month")
    public record MonthlyHoursResponse(
            @Schema(description = "Month as yyyy-MM", example = "2026-11")
            String month,
            @Schema(example = "1.5")
            double hours
    ) {
    }

    public static CompanyReportOverviewResponse from(CompanyReportOverview overview) {
        return new CompanyReportOverviewResponse(
                overview.completionRate(),
                overview.completionRateChange(),
                overview.averageHoursPerEmployee(),
                overview.certificatesThisQuarter(),
                overview.weeklyEngagement(),
                overview.completionByJobTitle().stream()
                        .map(title -> new JobTitleCompletionResponse(title.jobTitle(), title.completionRate()))
                        .toList(),
                overview.monthlyHours().stream()
                        .map(month -> new MonthlyHoursResponse(month.month().toString(), month.hours()))
                        .toList());
    }
}
