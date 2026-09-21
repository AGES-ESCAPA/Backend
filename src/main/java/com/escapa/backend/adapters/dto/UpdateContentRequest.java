package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.content.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateContentRequest(
        @NotBlank(message = "title is required") String title,
        @NotNull(message = "type is required") ContentType type,
        String url,
        Integer durationMinutes,
        String description,
        Boolean isFree
) {
}
