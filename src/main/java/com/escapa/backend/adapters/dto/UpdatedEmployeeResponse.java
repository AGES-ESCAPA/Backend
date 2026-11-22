package com.escapa.backend.adapters.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.UpdatedEmployee;
import com.escapa.backend.domain.user.UserStatus;

@Schema(description = "The employee after the edit, as listed in the employees table")
public record UpdatedEmployeeResponse(
        UUID id,
        @Schema(example = "Ana Paula Ferreira")
        String name,
        @Schema(example = "ana@empresa.com.br")
        String email,
        @Schema(example = "Recepção")
        String jobTitle,
        @Schema(description = "ACTIVE, PENDING (no password yet) or INACTIVE", example = "INACTIVE")
        UserStatus status,
        @Schema(description = "Enrollments that were not revoked in the courses of the company", example = "3")
        int assignedCourses,
        @Schema(description = "Null when the employee has no photo")
        String avatarUrl,
        @Schema(description = "Null when the edit issued no invite; otherwise whether the new invite e-mail went out",
                example = "true", nullable = true)
        Boolean inviteSent
) {

    public static UpdatedEmployeeResponse from(UpdatedEmployee updated, Boolean inviteSent) {
        final CompanyEmployee employee = updated.employee();
        return new UpdatedEmployeeResponse(
                employee.id(),
                employee.name(),
                employee.email(),
                employee.jobTitle(),
                employee.status(),
                employee.assignedCourses(),
                employee.avatarUrl(),
                inviteSent);
    }
}
