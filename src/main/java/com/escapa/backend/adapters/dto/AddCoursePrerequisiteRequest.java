package com.escapa.backend.adapters.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddCoursePrerequisiteRequest(
        @NotNull UUID prerequisiteCourseId
) {
}