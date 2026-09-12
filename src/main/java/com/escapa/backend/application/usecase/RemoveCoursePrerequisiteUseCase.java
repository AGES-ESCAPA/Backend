package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

public class RemoveCoursePrerequisiteUseCase {
    private final CourseRepositoryPort courseRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public RemoveCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.changeLogRepository = changeLogRepository;
    }

    public void execute(UUID courseId, UUID prerequisiteCourseId, UserEntity changedBy) {
        final CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
        if (!prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                courseId, prerequisiteCourseId)) {
            throw new IllegalArgumentException("Pré-requisito não encontrado.");
        }
        prerequisiteRepository.deleteByCourseIdAndPrerequisiteCourseId(
                courseId, prerequisiteCourseId);
        changeLogRepository.save(new CourseChangeLogEntity(
                null, course, changedBy, "Pré-requisito removido.",
                course.getMajorVersion(), course.getMinorVersion(), LocalDateTime.now()));
    }
}
