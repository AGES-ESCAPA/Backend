package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteId;
import jakarta.persistence.EntityManager;

@Repository
public class CoursePrerequisiteRepositoryAdapter
        implements CoursePrerequisiteRepositoryPort {
    private final CoursePrerequisiteJpaRepository coursePrerequisiteJpaRepository;
    private final EntityManager entityManager;

    public CoursePrerequisiteRepositoryAdapter(CoursePrerequisiteJpaRepository repository, EntityManager entityManager) {
        this.coursePrerequisiteJpaRepository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public List<CoursePrerequisiteEntity> findByCourseId(UUID courseId) {
        return coursePrerequisiteJpaRepository.findByCourseId(courseId);
    }

    @Override
    public List<CoursePrerequisiteEntity> findByPrerequisiteCourseId(UUID prerequisiteCourseId) {
        return coursePrerequisiteJpaRepository.findByPrerequisiteCourseId(prerequisiteCourseId);
    }

    @Override
    public void save(UUID courseId, UUID prerequisiteCourseId) {
        final CourseEntity course = entityManager.getReference(CourseEntity.class, courseId);
        final CourseEntity prerequisiteCourse = entityManager.getReference(CourseEntity.class, prerequisiteCourseId);
        coursePrerequisiteJpaRepository.save(new CoursePrerequisiteEntity(
                new CoursePrerequisiteId(courseId, prerequisiteCourseId), course, prerequisiteCourse));
    }

    @Override
    public boolean existsByCourseIdAndPrerequisiteCourseId(
            UUID courseId, UUID prerequisiteCourseId) {
        return coursePrerequisiteJpaRepository.existsByCourseIdAndPrerequisiteCourseId(
                courseId, prerequisiteCourseId);
    }

    @Override
    public void deleteByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId) {
        coursePrerequisiteJpaRepository.deleteById(
            new com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteId(
                courseId, prerequisiteCourseId));
    }
}
