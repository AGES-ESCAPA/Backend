package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;
import org.springframework.stereotype.Repository;

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
}
