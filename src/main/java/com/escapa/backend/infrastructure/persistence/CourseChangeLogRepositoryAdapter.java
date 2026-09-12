package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;

@Repository
public class CourseChangeLogRepositoryAdapter implements CourseChangeLogRepositoryPort {
    private final CourseChangeLogJpaRepository repository;
    private final EntityManager entityManager;

    public CourseChangeLogRepositoryAdapter(CourseChangeLogJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public List<CourseChangeLogEntity> findByCourseId(UUID courseId) {
        return repository.findByCourseIdOrderByCreatedAtDesc(courseId, PageRequest.of(0, 10)
        ).getContent();
    }

    @Override
    public void save(UUID courseId, UUID changedById, String description, int majorVersion, int minorVersion) {
        final CourseEntity course = entityManager.getReference(CourseEntity.class, courseId);
        final UserEntity changedBy = changedById != null
                ? entityManager.getReference(UserEntity.class, changedById)
                : null;
        repository.save(new CourseChangeLogEntity(
                null, course, changedBy, description, majorVersion, minorVersion, LocalDateTime.now()));
    }

    @Override
    public Page<CourseChangeLogEntity> findPageByCourseId(UUID courseId, Pageable pageable) {
        return repository.findByCourseIdOrderByCreatedAtDesc(courseId, pageable);
    }
}

