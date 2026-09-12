package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.adapters.dto.UpdateProgressRulesRequest;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.infrastructure.persistence.UserEntity;

public class UpdateProgressRulesUseCase {
    private final CourseRepositoryPort courseRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public UpdateProgressRulesUseCase(CourseRepositoryPort courseRepository) {
        this(courseRepository, null);
    }

    public UpdateProgressRulesUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        this.courseRepository = courseRepository;
        this.changeLogRepository = changeLogRepository;
    }

    public void execute(UUID courseId, UpdateProgressRulesRequest request) {
        execute(courseId, request, null);
    }

    public void execute(UUID courseId, UpdateProgressRulesRequest request, UserEntity changedBy) {
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));

        final boolean changed = !request.requireSequentialProgress().equals(
            course.getRequireSequentialProgress())
            || !request.enforceDeadlineBlock().equals(course.getEnforceDeadlineBlock());
        course.setRequireSequentialProgress(request.requireSequentialProgress());
        course.setEnforceDeadlineBlock(request.enforceDeadlineBlock());
        if (changed && course.getStatus() == CourseStatus.PUBLISHED) {
            course.setMinorVersion(course.getMinorVersion() + 1);
        }
        courseRepository.save(course);
        if (changed && changeLogRepository != null) {
            changeLogRepository.save(courseId, changedBy != null ? changedBy.getId() : null,
                    "Alteração nas regras de progressão.",
                    course.getMajorVersion(), course.getMinorVersion());
        }
    }
}