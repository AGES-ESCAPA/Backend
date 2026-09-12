package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;

public interface CourseChangeLogRepositoryPort {
    List<CourseChangeLogEntity> findByCourseId(UUID courseId);

    Page<CourseChangeLogEntity> findPageByCourseId(UUID courseId, Pageable pageable);

    void save(UUID courseId, UUID changedById, String description, int majorVersion, int minorVersion);
}
