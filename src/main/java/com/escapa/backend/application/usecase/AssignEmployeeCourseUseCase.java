package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import com.escapa.backend.application.model.AcquiredCourse;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.OrderAccessPort;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;
import com.escapa.backend.domain.course.CourseNotFoundException;

/**
 * Libera um curso da empresa para o colaborador (matrícula ativa ou reativada).
 */
public class AssignEmployeeCourseUseCase {

    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;
    private final CompanyCourseRepositoryPort companyCourseRepositoryPort;
    private final OrderAccessPort orderAccessPort;
    private final Clock clock;

    public AssignEmployeeCourseUseCase(
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort,
            CompanyCourseRepositoryPort companyCourseRepositoryPort,
            OrderAccessPort orderAccessPort,
            Clock clock
    ) {
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
        this.companyCourseRepositoryPort = companyCourseRepositoryPort;
        this.orderAccessPort = orderAccessPort;
        this.clock = clock;
    }

    public void execute(UUID companyId, UUID employeeId, UUID courseId) {
        if (companyId == null || employeeId == null || courseId == null) {
            throw new IllegalArgumentException("Company, employee and course are required");
        }
        if (!companyEmployeeRepositoryPort.isEmployeeOfCompany(companyId, employeeId)) {
            throw new CompanyEmployeeNotFoundException(companyId, employeeId);
        }

        final AcquiredCourse course = companyCourseRepositoryPort.findAcquiredCourses(companyId).stream()
                .filter(acquired -> acquired.courseId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        final LocalDate today = LocalDate.now(clock);
        final LocalDate expiration = course.deadline() == null ? null : today.plusDays(course.deadline());
        orderAccessPort.saveStudentEnrollment(employeeId, courseId, today, expiration);
    }
}
