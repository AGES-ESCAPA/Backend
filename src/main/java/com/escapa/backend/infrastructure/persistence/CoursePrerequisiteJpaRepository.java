package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteId;

public interface CoursePrerequisiteJpaRepository
        extends JpaRepository<CoursePrerequisiteEntity, CoursePrerequisiteId> {

    List<CoursePrerequisiteEntity> findByCourseId(UUID courseId);

    List<CoursePrerequisiteEntity> findByPrerequisiteCourseId(
            UUID prerequisiteCourseId
    );

    boolean existsByCourseIdAndPrerequisiteCourseId(
            UUID courseId,
            UUID prerequisiteCourseId
    );
}