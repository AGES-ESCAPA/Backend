package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

@Schema(description = "Course that must be completed first")
public record AddCoursePrerequisiteRequest(
        @NotNull(message = "prerequisiteCourseId is required") UUID prerequisiteCourseId
) {
}