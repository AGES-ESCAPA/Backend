package com.escapa.backend.adapters.dto.course;

import com.escapa.backend.adapters.dto.FieldLimits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateCourseRequest(
        @Size(max = FieldLimits.VARCHAR_MAX, message = "title " + FieldLimits.VARCHAR_MESSAGE)
        String title,
        String shortDescription,
        String description,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "thumbnailUrl " + FieldLimits.VARCHAR_MESSAGE)
        String thumbnailUrl,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "teaserVideoUrl " + FieldLimits.VARCHAR_MESSAGE)
        String teaserVideoUrl,
        UUID instructorId,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "category " + FieldLimits.VARCHAR_MESSAGE)
        String category,
        @Size(max = FieldLimits.VARCHAR_MAX, message = "level " + FieldLimits.VARCHAR_MESSAGE)
        String level,
        @PositiveOrZero(message = "durationTime must not be negative")
        Integer durationTime,
        @PositiveOrZero(message = "deadline must not be negative")
        Integer deadline,
        @PositiveOrZero(message = "accessDurationDays must not be negative")
        Integer accessDurationDays,
        @PositiveOrZero(message = "price must not be negative")
        Double price,
        List<String> learningObjectives,
        Boolean requireSequentialProgress,
        Boolean enforceDeadlineBlock
) {}

