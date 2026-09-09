package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;

public interface CourseChangeLogRepositoryPort {
    List<CourseChangeLogEntity> findByCourseId(UUID courseId);
}
