package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Progress rules of a course")
public record UpdateProgressRulesRequest(
        @NotNull(message = "requireSequentialProgress is required") Boolean requireSequentialProgress,
        @NotNull(message = "enforceDeadlineBlock is required") Boolean enforceDeadlineBlock,
        @NotNull(message = "minQuizPassPercentage is required")
        @Min(value = 0, message = "minQuizPassPercentage must be between 0 and 100")
        @Max(value = 100, message = "minQuizPassPercentage must be between 0 and 100")
        Integer minQuizPassPercentage
) {
}