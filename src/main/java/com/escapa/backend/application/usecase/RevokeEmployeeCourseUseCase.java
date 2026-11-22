package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.port.CompanyCourseRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeUnlinkPort;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;
import com.escapa.backend.domain.course.CourseNotFoundException;

/**
 * Remove o acesso do colaborador a um curso da empresa, sem apagar progresso nem compra individual.
 */
public class RevokeEmployeeCourseUseCase {

    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;
    private final CompanyCourseRepositoryPort companyCourseRepositoryPort;
    private final CompanyEmployeeUnlinkPort companyEmployeeUnlinkPort;
    private final Clock clock;

    public RevokeEmployeeCourseUseCase(
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort,
            CompanyCourseRepositoryPort companyCourseRepositoryPort,
            CompanyEmployeeUnlinkPort companyEmployeeUnlinkPort,
            Clock clock
    ) {
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
        this.companyCourseRepositoryPort = companyCourseRepositoryPort;
        this.companyEmployeeUnlinkPort = companyEmployeeUnlinkPort;
        this.clock = clock;
    }

    public void execute(UUID companyId, UUID employeeId, UUID courseId) {
        if (companyId == null || employeeId == null || courseId == null) {
            throw new IllegalArgumentException("Company, employee and course are required");
        }
        if (!companyEmployeeRepositoryPort.isEmployeeOfCompany(companyId, employeeId)) {
            throw new CompanyEmployeeNotFoundException(companyId, employeeId);
        }
        final boolean owned = companyCourseRepositoryPort.findAcquiredCourseIds(companyId).contains(courseId);
        if (!owned) {
            throw new CourseNotFoundException(courseId);
        }
        companyEmployeeUnlinkPort.revokeCompanyCourseEnrollment(
                companyId, employeeId, courseId, LocalDateTime.now(clock));
    }
}
