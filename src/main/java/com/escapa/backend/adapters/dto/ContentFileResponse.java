package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lesson file upload result")
public record ContentFileResponse(String url) {
}
