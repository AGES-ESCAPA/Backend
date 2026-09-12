package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;

@Repository
public class CourseChangeLogRepositoryAdapter implements CourseChangeLogRepositoryPort {
    private final CourseChangeLogJpaRepository repository;;

    public CourseChangeLogRepositoryAdapter(CourseChangeLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CourseChangeLogEntity> findByCourseId(UUID courseId) {
        return repository.findByCourseIdOrderByCreatedAtDesc(courseId, PageRequest.of(0, 10)
        ).getContent();
    }

    @Override
    public CourseChangeLogEntity save(CourseChangeLogEntity changeLog) {
        return repository.save(changeLog);
    }

    @Override
    public Page<CourseChangeLogEntity> findPageByCourseId(UUID courseId, Pageable pageable) {
        return repository.findByCourseIdOrderByCreatedAtDesc(courseId, pageable);
    }
}

