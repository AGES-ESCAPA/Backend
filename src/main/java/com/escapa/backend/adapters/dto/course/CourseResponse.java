package com.escapa.backend.adapters.dto.course;

import com.escapa.backend.domain.course.CourseStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String title,
        String shortDescription,
        String description,
        String thumbnailUrl,
        String teaserVideoUrl,
        CourseStatus status,
        UUID instructorId,
        UUID createdBy,
        String category,
        String level,
        Integer durationTime,
        Integer deadline,
        Integer accessDurationDays,
        Double price,
        List<String> learningObjectives,
        Boolean requireSequentialProgress,
        Boolean enforceDeadlineBlock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

