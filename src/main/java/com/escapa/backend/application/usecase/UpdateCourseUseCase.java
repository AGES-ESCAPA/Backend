package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.adapters.dto.UpdateCourseRequest;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;

public class UpdateCourseUseCase {
    private final CourseRepositoryPort courseRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public UpdateCourseUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        this.courseRepository = courseRepository;
        this.changeLogRepository = changeLogRepository;
    }

    public CourseEntity execute(UUID courseId, UpdateCourseRequest request, UserEntity changedBy) {
        final CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
        final boolean changed = !request.title().equals(course.getTitle())
                || !java.util.Objects.equals(request.description(), course.getDescription());
        course.setTitle(request.title());
        course.setDescription(request.description());
        if (changed && course.getStatus() == CourseStatus.PUBLISHED) {
            course.setMinorVersion(course.getMinorVersion() + 1);
            changeLogRepository.save(new CourseChangeLogEntity(
                    null, course, changedBy, "Curso atualizado.", course.getMajorVersion(),
                    course.getMinorVersion(), LocalDateTime.now()));
        }
        return courseRepository.save(course);
    }
}
