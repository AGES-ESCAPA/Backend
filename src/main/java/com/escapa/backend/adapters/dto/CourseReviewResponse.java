package com.escapa.backend.adapters.dto;

import java.util.UUID;

public record CourseReviewResponse(UUID courseId, Integer rating) {
}
