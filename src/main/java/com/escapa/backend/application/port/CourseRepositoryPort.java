package com.escapa.backend.application.port;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepositoryPort {

    Optional<CourseEntity> findById(UUID id);

    CourseEntity save(CourseEntity course);
}
