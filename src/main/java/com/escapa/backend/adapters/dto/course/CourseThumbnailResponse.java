package com.escapa.backend.adapters.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public URL of a course cover image stored in the media bucket")
public record CourseThumbnailResponse(String url) {
}
