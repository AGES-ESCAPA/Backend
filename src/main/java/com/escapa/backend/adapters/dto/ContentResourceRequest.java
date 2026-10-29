package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.content.ContentResource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Material complementar; obrigatoriedade e formato validados em ContentTypeRules. */
@Schema(description = "Supplementary lesson material: an external link or a file already uploaded to storage")
public record ContentResourceRequest(
        @Size(max = FieldLimits.VARCHAR_MAX, message = "resources.id " + FieldLimits.VARCHAR_MESSAGE)
        String id,
        @Schema(allowableValues = {"link", "file"})
        String type,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "resources.title " + FieldLimits.VARCHAR_MESSAGE)
        String title,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "resources.url " + FieldLimits.VARCHAR_MESSAGE)
        String url
) {
    public ContentResource toDomain() {
        return new ContentResource(id, type, title, url);
    }
}
