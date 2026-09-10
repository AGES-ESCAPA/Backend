package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.adapters.dto.UpdateProgressRulesRequest;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

public class UpdateProgressRulesUseCase {
    private final CourseRepositoryPort courseRepository;

    public UpdateProgressRulesUseCase(CourseRepositoryPort courseRepository) {
        this.courseRepository = courseRepository;
    }

    public void execute(UUID courseId, UpdateProgressRulesRequest request) {
        final CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));

        course.setRequireSequentialProgress(request.requireSequentialProgress());
        course.setEnforceDeadlineBlock(request.enforceDeadlineBlock());
        courseRepository.save(course);
    }
}