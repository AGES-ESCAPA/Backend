package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteId;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

public class CoursePrerequisiteRepositoryAdapter implements CoursePrerequisiteRepositoryPort {

    private final CoursePrerequisiteJpaRepository coursePrerequisiteJpaRepository;
    private final EntityManager entityManager;

    public CoursePrerequisiteRepositoryAdapter(
            CoursePrerequisiteJpaRepository coursePrerequisiteJpaRepository, EntityManager entityManager) {
        this.coursePrerequisiteJpaRepository = coursePrerequisiteJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public List<UUID> findPrerequisiteCourseIds(UUID courseId) {
        return coursePrerequisiteJpaRepository.findByCourseId(courseId).stream()
                .map(link -> link.getPrerequisiteCourse().getId())
                .toList();
    }

    @Override
    public boolean existsByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId) {
        return coursePrerequisiteJpaRepository.existsByCourseIdAndPrerequisiteCourseId(
                courseId, prerequisiteCourseId);
    }

    @Override
    public void save(UUID courseId, UUID prerequisiteCourseId) {
        final CourseEntity course = entityManager.getReference(CourseEntity.class, courseId);
        final CourseEntity prerequisiteCourse = entityManager.getReference(CourseEntity.class, prerequisiteCourseId);
        coursePrerequisiteJpaRepository.save(new CoursePrerequisiteEntity(
                new CoursePrerequisiteId(courseId, prerequisiteCourseId), course, prerequisiteCourse));
    }

    @Override
    public void deleteByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId) {
        coursePrerequisiteJpaRepository.deleteById(new CoursePrerequisiteId(courseId, prerequisiteCourseId));
    }
}
