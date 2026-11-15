package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.dto.CompanyCourseSummary;

import java.time.LocalDate;
import java.util.UUID;

public record CompanyCourseResponse(
        UUID id,
        String title,
        String shortDescription,
        String thumbnailUrl,
        String category,
        String level,
        Integer durationTime,
        Integer lessonsCount,
        String instructorName,
        Double rating,
        LocalDate acquiredAt,
        Long employeesWithAccess
) {
    public static CompanyCourseResponse from(CompanyCourseSummary summary) {
        return new CompanyCourseResponse(
                summary.id(),
                summary.title(),
                summary.shortDescription(),
                summary.thumbnailUrl(),
                summary.category(),
                summary.level(),
                summary.durationTime(),
                summary.lessonsCount(),
                summary.instructorName(),
                summary.rating(),
                summary.acquiredAt(),
                summary.employeesWithAccess()
        );
    }
}