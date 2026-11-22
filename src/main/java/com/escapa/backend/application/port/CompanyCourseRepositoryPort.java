package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.dto.CompanyCourseSummary;
import com.escapa.backend.application.model.AcquiredCourse;
import com.escapa.backend.application.model.EmployeeCourseAssignment;

public interface CompanyCourseRepositoryPort {

    /** Cursos comprados pela empresa, ativos ou vencidos; um curso revogado pelo cancelamento do pedido fica de fora. */
    List<UUID> findAcquiredCourseIds(UUID companyId);
    List<CompanyCourseSummary> findActiveCourses(UUID companyId);

    /** Cursos da empresa sem {@code revoked_at}, com o prazo de cada um: os que um novo colaborador recebe. */
    List<AcquiredCourse> findAcquiredCourses(UUID companyId);

    /**
     * Cursos publicados da empresa com flag de matrícula ativa do colaborador.
     * Cursos arquivados ou rascunho ficam de fora.
     */
    List<EmployeeCourseAssignment> findEmployeeCourseAssignments(UUID companyId, UUID employeeId);
}
