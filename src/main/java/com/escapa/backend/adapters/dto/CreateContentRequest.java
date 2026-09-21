package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.content.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Campos condicionais por tipo (url, durationMinutes, description) sao validados
 * em ContentTypeRules, porque a obrigatoriedade depende do valor de {@code type}.
 */
public record CreateContentRequest(
        @NotBlank(message = "title is required") String title,
        @NotNull(message = "type is required") ContentType type,
        String url,
        Integer durationMinutes,
        String description,
        Boolean isFree
) {
}
