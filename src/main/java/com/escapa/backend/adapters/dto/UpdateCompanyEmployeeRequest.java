package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.user.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Data to edit an employee of the company")
public record UpdateCompanyEmployeeRequest(
        @NotBlank(message = "name is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "name " + FieldLimits.VARCHAR_MESSAGE)
        @Schema(example = "Ana Paula Ferreira")
        String name,
        @NotBlank(message = "email is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "email " + FieldLimits.VARCHAR_MESSAGE)
        @Schema(example = "ana@empresa.com.br")
        String email,
        @NotBlank(message = "jobTitle is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "jobTitle " + FieldLimits.VARCHAR_MESSAGE)
        @Schema(example = "Recepção")
        String jobTitle,
        @NotNull(message = "status is required")
        @Schema(description = "ACTIVE or INACTIVE. An employee without a password stays PENDING when set ACTIVE",
                example = "INACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        UserStatus status
) {
}
