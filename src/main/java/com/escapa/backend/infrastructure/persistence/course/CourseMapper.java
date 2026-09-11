package com.escapa.backend.infrastructure.persistence.course;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.course.shared.entity.CourseEntity;

/**
 * Conversor estático entre {@link CourseEntity} (JPA) e {@link CourseSummary} (aplicação).
 * Segue o mesmo padrão do {@code UserMapper} existente.
 */
public final class CourseMapper {

    private CourseMapper() {
    }

    public static CourseSummary toSummary(CourseEntity entity) {
        if (entity == null) {
            return null;
        }
        final String instructorName = entity.getInstructor() != null
                ? entity.getInstructor().getName() : null;

        return new CourseSummary(
                entity.getId(),
                entity.getTitle(),
                entity.getShortDescription(),
                entity.getCategory(),
                entity.getLevel(),
                entity.getDurationTime(),
                entity.getLessonsCount(),
                entity.getPrice(),
                entity.getThumbnailUrl(),
                instructorName,
                entity.getRatingAverage(),
                entity.getReviewsCount()
        );
    }
}
