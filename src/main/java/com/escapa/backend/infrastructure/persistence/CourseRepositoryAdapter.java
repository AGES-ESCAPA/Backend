package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

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
    public CourseEntity save(CourseEntity course) {
        return courseJpaRepository.save(course);
    }
}
