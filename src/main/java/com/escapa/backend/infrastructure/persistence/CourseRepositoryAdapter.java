package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

@Repository
public class CourseRepositoryAdapter implements CourseRepositoryPort{

    private final CourseJpaRepository courseJpaRepository;

    public CourseRepositoryAdapter(CourseJpaRepository courseJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
    }

    @Override
    public Optional<CourseEntity> findById(UUID id) {
        return courseJpaRepository.findById(id);
    }

    @Override
    public List<CourseEntity> searchByTitle(String query, UUID excludedCourseId) {
        return courseJpaRepository.findByTitleContainingIgnoreCaseAndIdNot(query, excludedCourseId);
    }

    @Override
    public CourseEntity save(CourseEntity course) {
        return courseJpaRepository.save(course);
    }
}
