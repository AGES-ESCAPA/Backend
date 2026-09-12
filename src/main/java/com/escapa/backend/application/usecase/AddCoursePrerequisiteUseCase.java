package com.escapa.backend.application.usecase;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.escapa.backend.adapters.dto.AddCoursePrerequisiteRequest;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.infrastructure.persistence.UserEntity;

public class AddCoursePrerequisiteUseCase {
    private final CourseRepositoryPort courseRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public AddCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository) {
        this(courseRepository, prerequisiteRepository, null);
    }

    public AddCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.changeLogRepository = changeLogRepository;
    }

    public void execute(UUID courseId, AddCoursePrerequisiteRequest request) {
        execute(courseId, request, null);
    }

    public void execute(UUID courseId, AddCoursePrerequisiteRequest request, UserEntity changedBy) {
        final UUID prerequisiteCourseId = request.prerequisiteCourseId();
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
        courseRepository.findById(prerequisiteCourseId)
                .orElseThrow(() -> new IllegalArgumentException("Pré-requisito não encontrado."));

        if (courseId.equals(prerequisiteCourseId)) {
            throw new IllegalArgumentException("Um curso não pode ser pré-requisito dele mesmo.");
        }
        if (prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                courseId, prerequisiteCourseId)) {
            throw new IllegalArgumentException("Pré-requisito já cadastrado.");
        }
        if (createsCycle(courseId, prerequisiteCourseId, new HashSet<>())) {
            throw new IllegalArgumentException("O pré-requisito formaria um ciclo.");
        }

        prerequisiteRepository.save(courseId, prerequisiteCourseId);
        if (changeLogRepository != null) {
            changeLogRepository.save(courseId, changedBy != null ? changedBy.getId() : null,
                    "Pré-requisito adicionado.", course.getMajorVersion(), course.getMinorVersion());
        }
    }

    private boolean createsCycle(UUID courseId, UUID candidate, Set<UUID> visited) {
        if (courseId.equals(candidate)) {
            return true;
        }
        if (!visited.add(candidate)) {
            return false;
        }
        return prerequisiteRepository.findByCourseId(candidate).stream()
            .anyMatch(link -> createsCycle(courseId, link.getPrerequisiteCourse().getId(), visited));
    }
}