package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;

@Repository
public class CoursePrerequisiteRepositoryAdapter
        implements CoursePrerequisiteRepositoryPort {
    private final CoursePrerequisiteJpaRepository coursePrerequisiteJpaRepository;

    public CoursePrerequisiteRepositoryAdapter(CoursePrerequisiteJpaRepository repository) {
        this.coursePrerequisiteJpaRepository = repository;
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
    public CoursePrerequisiteEntity save(CoursePrerequisiteEntity prerequisite) {
        return coursePrerequisiteJpaRepository.save(prerequisite);
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
