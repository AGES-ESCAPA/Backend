package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProgressRulesRequest(
        @NotNull Boolean requireSequentialProgress,
        @NotNull Boolean enforceDeadlineBlock
) {
}