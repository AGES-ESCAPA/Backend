package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;

public class UnarchiveCourseUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnarchiveCourseUseCase.class);

    private final CourseRepositoryPort courseRepositoryPort;

    public UnarchiveCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    /**
     * Restaura um curso arquivado para o catálogo ativo como {@link CourseStatus#PUBLISHED}.
     */
    public Course execute(UUID id) {
        final Course course = courseRepositoryPort.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
        if (course.getStatus() != CourseStatus.ARCHIVED) {
            throw new IllegalArgumentException("Only archived courses can be unarchived");
        }
        course.setStatus(CourseStatus.PUBLISHED);
        course.setUpdatedAt(LocalDateTime.now());
        final Course saved = courseRepositoryPort.save(course);
        LOGGER.info("Course {} unarchived as PUBLISHED", saved.getId());
        return saved;
    }
}
