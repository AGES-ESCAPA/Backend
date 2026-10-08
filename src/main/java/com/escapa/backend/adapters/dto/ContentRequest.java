package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.content.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de criação e edição de aula. Campos condicionais por tipo (url, durationMinutes,
 * description) sao validados em ContentTypeRules, porque a obrigatoriedade depende do valor
 * de {@code type}.
 */
public record ContentRequest(
        @NotBlank(message = "title is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "title " + FieldLimits.VARCHAR_MESSAGE)
        String title,
        @NotNull(message = "type is required") ContentType type,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "url " + FieldLimits.VARCHAR_MESSAGE)
        String url,
        Integer durationMinutes,
        String description,
        Boolean isFree
) {
}
