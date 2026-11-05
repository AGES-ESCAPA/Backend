package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.domain.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "An employee linked to the company")
public record CompanyEmployeeResponse(
        UUID id,
        @Schema(example = "Ana Paula Ferreira")
        String name,
        @Schema(example = "ana.ferreira@empresa.com.br")
        String email,
        @Schema(description = "Job title, kept in users_company.role", example = "Recepção")
        String jobTitle,
        @Schema(description = "ACTIVE, PENDING (invite not accepted yet) or INACTIVE", example = "ACTIVE")
        UserStatus status,
        @Schema(description = "Enrollments that were not revoked in the courses of the company", example = "3")
        int assignedCourses,
        @Schema(description = "Null when the employee has no photo")
        String avatarUrl
) {

    public static CompanyEmployeeResponse from(CompanyEmployee employee) {
        return new CompanyEmployeeResponse(
                employee.id(),
                employee.name(),
                employee.email(),
                employee.jobTitle(),
                employee.status(),
                employee.assignedCourses(),
                employee.avatarUrl());
    }
}
