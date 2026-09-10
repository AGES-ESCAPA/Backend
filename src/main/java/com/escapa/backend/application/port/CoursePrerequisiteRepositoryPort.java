package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;

public interface CoursePrerequisiteRepositoryPort {

    List<CoursePrerequisiteEntity> findByCourseId(UUID courseId);

    CoursePrerequisiteEntity save(CoursePrerequisiteEntity prerequisite);

    boolean existsByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId);
}
