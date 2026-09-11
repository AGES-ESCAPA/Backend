package com.escapa.backend.course.management.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddCoursePrerequisiteRequest(
        @NotNull(message = "prerequisiteCourseId is required") UUID prerequisiteCourseId
) {
}