package com.escapa.backend.adapters.dto.course;

import java.util.List;
import java.util.UUID;

public record UpdateCourseRequest(
        String title,
        String shortDescription,
        String description,
        String thumbnailUrl,
        String teaserVideoUrl,
        UUID instructorId,
        String category,
        String level,
        Integer durationTime,
        Integer deadline,
        Integer accessDurationDays,
        Double price,
        List<String> learningObjectives,
        Boolean requireSequentialProgress,
        Boolean enforceDeadlineBlock
) {}

