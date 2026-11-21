package com.escapa.backend.application.port;

import java.time.LocalDate;
import java.util.List;

import com.escapa.backend.application.model.CompanyEngagement;
import com.escapa.backend.application.model.EnrollmentCompletionCounts;

public interface AdminMetricsRepositoryPort {
    long countActiveStudents(LocalDate date);

    long countPartnerCompanies();

    /** Empresas ativas criadas antes do início do mês corrente. */
    long countPartnerCompaniesCreatedBefore(LocalDate startOfCurrentMonth);

    EnrollmentCompletionCounts countHistoricalCompletion();

    long countActiveStudentsInPeriod(LocalDate from, LocalDate to);

    List<CompanyEngagement> findTopEngagedCompanies(int limit);
}
