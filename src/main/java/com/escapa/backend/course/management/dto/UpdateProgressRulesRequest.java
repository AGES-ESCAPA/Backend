package com.escapa.backend.course.management.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProgressRulesRequest(
        @NotNull(message = "requireSequentialProgress is required") Boolean requireSequentialProgress,
        @NotNull(message = "enforceDeadlineBlock is required") Boolean enforceDeadlineBlock
) {
}