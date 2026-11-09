package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.CompanyCourseSummary;
import java.util.List;
import java.util.UUID;

public interface CompanyCourseRepositoryPort {

    /** Cursos comprados pela empresa, ativos ou vencidos; um curso revogado pelo cancelamento do pedido fica de fora. */
    List<UUID> findAcquiredCourseIds(UUID companyId);
    List<CompanyCourseSummary> findActiveCourses(UUID companyId);
}
