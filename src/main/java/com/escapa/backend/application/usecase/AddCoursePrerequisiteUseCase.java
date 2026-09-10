package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.adapters.dto.AddCoursePrerequisiteRequest;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteId;

public class AddCoursePrerequisiteUseCase {
    private final CourseRepositoryPort courseRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;

    public AddCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository) {
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public void execute(UUID courseId, AddCoursePrerequisiteRequest request) {
        final UUID prerequisiteCourseId = request.prerequisiteCourseId();
        final CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
        final CourseEntity prerequisiteCourse = courseRepository.findById(prerequisiteCourseId)
                .orElseThrow(() -> new IllegalArgumentException("Pré-requisito não encontrado."));

        if (courseId.equals(prerequisiteCourseId)) {
            throw new IllegalArgumentException("Um curso não pode ser pré-requisito dele mesmo.");
        }
        if (prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                courseId, prerequisiteCourseId)) {
            throw new IllegalArgumentException("Pré-requisito já cadastrado.");
        }

        prerequisiteRepository.save(new CoursePrerequisiteEntity(
                new CoursePrerequisiteId(courseId, prerequisiteCourseId),
                course,
                prerequisiteCourse));
    }
}