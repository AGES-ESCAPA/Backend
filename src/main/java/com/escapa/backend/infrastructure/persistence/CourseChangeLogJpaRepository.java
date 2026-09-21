package com.escapa.backend.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;

public interface CourseChangeLogJpaRepository
        extends JpaRepository<CourseChangeLogEntity, UUID> {

    Page<CourseChangeLogEntity> findByCourseIdOrderByCreatedAtDesc(
            UUID courseId,
            Pageable pageable
    );
}