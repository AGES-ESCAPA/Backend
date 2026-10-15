package com.escapa.backend.adapters.dto.course;

import com.escapa.backend.domain.entity.Course;
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
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                course.getThumbnailUrl(),
                course.getTeaserVideoUrl(),
                course.getStatus(),
                course.getInstructor() != null ? course.getInstructor().getId() : null,
                course.getCreatedBy() != null ? course.getCreatedBy().getId() : null,
                course.getCategory(),
                course.getLevel(),
                course.getDurationTime(),
                course.getDeadline(),
                course.getAccessDurationDays(),
                course.getPrice(),
                course.getLearningObjectives(),
                course.getRequireSequentialProgress(),
                course.getEnforceDeadlineBlock(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
