package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.StudentCourseCard;

import java.util.UUID;

public record StudentCourseCardResponse(
        UUID courseId,
        String title,
        String instructor,
        String thumbnailUrl,
        Integer durationTime,
        Integer lessonsCount,
        Integer progressPercentage,
        String enrollmentStatus
) {
    public static StudentCourseCardResponse from(StudentCourseCard card) {
        return new StudentCourseCardResponse(
                card.courseId(),
                card.title(),
                card.instructor(),
                card.thumbnailUrl(),
                card.durationTime(),
                card.lessonsCount(),
                card.progressPercentage(),
                card.enrollmentStatus() != null ? card.enrollmentStatus().name() : null
        );
    }
}

