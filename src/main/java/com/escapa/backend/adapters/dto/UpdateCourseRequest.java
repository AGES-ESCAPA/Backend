package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCourseRequest(
        @NotBlank(message = "title is required") String title,
        String description
) {
}
