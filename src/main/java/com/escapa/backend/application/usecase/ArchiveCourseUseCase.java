package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;

public class ArchiveCourseUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveCourseUseCase.class);

    private final CourseRepositoryPort courseRepositoryPort;

    public ArchiveCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    /**
     * Arquiva o curso e descarta o rascunho pendente.
     * Sem isso, edições feitas após o arquivamento (gravadas nas colunas) podem ser
     * sobrescritas por um rascunho antigo na próxima publicação.
     */
    public Course execute(UUID id) {
        final Course course = courseRepositoryPort.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
        course.setStatus(CourseStatus.ARCHIVED);
        course.setPendingDraft(null);
        course.setUpdatedAt(LocalDateTime.now());
        final Course saved = courseRepositoryPort.save(course);
        LOGGER.info("Course {} archived", saved.getId());
        return saved;
    }
}
