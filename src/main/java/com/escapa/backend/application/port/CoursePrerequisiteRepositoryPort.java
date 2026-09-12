package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;

public interface CoursePrerequisiteRepositoryPort {

    List<CoursePrerequisiteEntity> findByCourseId(UUID courseId);

    List<CoursePrerequisiteEntity> findByPrerequisiteCourseId(UUID prerequisiteCourseId);

    void save(UUID courseId, UUID prerequisiteCourseId);

    boolean existsByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId);

    void deleteByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId);
}
