package com.escapa.backend.application.port;
import java.util.List;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

public interface CourseRepositoryPort {

    Optional<CourseEntity> findById(UUID id);

    List<CourseEntity> searchByTitle(String query, UUID excludedCourseId);

    CourseEntity save(CourseEntity course);
}
