package com.escapa.backend.application.port;

import com.escapa.backend.application.model.EnrollmentCompletionCounts;
import java.time.LocalDate;

public interface AdminMetricsRepositoryPort {
    long countActiveStudents(LocalDate date);

    long countPartnerCompanies();

    EnrollmentCompletionCounts countHistoricalCompletion();

    long countActiveStudentsInPeriod(LocalDate from, LocalDate to);
}
