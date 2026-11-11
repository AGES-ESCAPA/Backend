package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.CompanyEmployeeProgress;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Progress of one employee in one course of the company")
public record CompanyEmployeeProgressResponse(
        UUID userId,
        @Schema(example = "Ana Paula Ferreira")
        String name,
        @Schema(description = "Null when the employee has no photo")
        String avatarUrl,
        UUID courseId,
        @Schema(example = "Fundamentos de IA para o Turismo")
        String course,
        @Schema(description = "0 to 100", example = "40")
        int progress,
        @Schema(description = "Null when the employee never opened a lesson of the course")
        LocalDateTime lastAccessAt,
        boolean certificateIssued
) {

    public static CompanyEmployeeProgressResponse from(CompanyEmployeeProgress row) {
        return new CompanyEmployeeProgressResponse(
                row.userId(),
                row.name(),
                row.avatarUrl(),
                row.courseId(),
                row.courseTitle(),
                row.progress(),
                row.lastAccessAt(),
                row.certificateIssued());
    }
}
