package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.content.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Corpo de criação e edição de aula. Campos condicionais por tipo (url, durationMinutes,
 * description) sao validados em ContentTypeRules, porque a obrigatoriedade depende do valor
 * de {@code type}.
 */
@Schema(description = "Lesson create/update payload; url, durationMinutes and description depend on the type")
public record ContentRequest(
        @NotBlank(message = "title is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "title " + FieldLimits.VARCHAR_MESSAGE)
        String title,
        @NotNull(message = "type is required") ContentType type,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "url " + FieldLimits.VARCHAR_MESSAGE)
        String url,
        Integer durationMinutes,
        String description,
        Boolean isFree,
        @Valid List<ContentResourceRequest> resources
) {
    public List<ContentResource> resourcesToDomain() {
        return resources == null
                ? List.of()
                : resources.stream().map(ContentResourceRequest::toDomain).toList();
    }
}
