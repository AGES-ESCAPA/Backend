package com.escapa.backend.application.usecase;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.model.EmployeeCourseAssignment;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;

/**
 * Lista os cursos da empresa e marca quais estão liberados para o colaborador.
 */
public class ListEmployeeCoursesUseCase {

    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;
    private final CompanyCourseRepositoryPort companyCourseRepositoryPort;

    public ListEmployeeCoursesUseCase(
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort,
            CompanyCourseRepositoryPort companyCourseRepositoryPort
    ) {
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
        this.companyCourseRepositoryPort = companyCourseRepositoryPort;
    }

    public List<EmployeeCourseAssignment> execute(UUID companyId, UUID employeeId) {
        if (companyId == null || employeeId == null) {
            throw new IllegalArgumentException("Company and employee are required");
        }
        if (!companyEmployeeRepositoryPort.isEmployeeOfCompany(companyId, employeeId)) {
            throw new CompanyEmployeeNotFoundException(companyId, employeeId);
        }
        return companyCourseRepositoryPort.findEmployeeCourseAssignments(companyId, employeeId);
    }
}
