package com.escapa.backend.application.port;

import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;
import java.util.List;
import java.util.UUID;

public interface CoursePrerequisiteRepositoryPort {

    List<CoursePrerequisiteEntity> findByCourseId(UUID courseId);
}
