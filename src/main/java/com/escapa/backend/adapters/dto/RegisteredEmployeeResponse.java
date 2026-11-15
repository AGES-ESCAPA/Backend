package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.RegisteredEmployee;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "The employee just registered and whether the invite e-mail went out")
public record RegisteredEmployeeResponse(
        UUID id,
        @Schema(example = "João da Silva")
        String name,
        @Schema(example = "joao.silva@empresa.com.br")
        String email,
        @Schema(description = "Always PENDING: the employee has no password until the invite link is used", example = "PENDING")
        UserStatus status,
        @Schema(description = "Courses of the company the employee was enrolled in", example = "2")
        int assignedCourses,
        @Schema(description = "Null when the employee has no photo")
        String avatarUrl,
        @Schema(description = "False when the e-mail failed to send; the manager can resend the invite", example = "true")
        boolean inviteSent
) {

    public static RegisteredEmployeeResponse from(RegisteredEmployee registered, boolean inviteSent) {
        final User employee = registered.employee();
        return new RegisteredEmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getStatus(),
                registered.assignedCourses(),
                employee.getAvatarUrl(),
                inviteSent);
    }
}
