package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Publish options; the body is optional")
public record PublishCourseRequest(Boolean notifyEnrolledStudents) {
}
