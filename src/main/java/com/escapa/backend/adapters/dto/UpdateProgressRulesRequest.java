package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Progress rules of a course")
public record UpdateProgressRulesRequest(
        @NotNull(message = "requireSequentialProgress is required") Boolean requireSequentialProgress,
        @NotNull(message = "enforceDeadlineBlock is required") Boolean enforceDeadlineBlock
) {
}