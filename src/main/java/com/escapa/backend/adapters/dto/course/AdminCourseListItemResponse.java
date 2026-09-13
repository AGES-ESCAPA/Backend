package com.escapa.backend.adapters.dto.course;

import com.escapa.backend.domain.course.CourseStatus;

import java.util.UUID;

public record AdminCourseListItemResponse(
        UUID id,
        String title,
        String category,
        Double price,
        CourseStatus status,
        Integer majorVersion,
        Integer minorVersion
) {}
