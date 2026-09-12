package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {

    @EntityGraph(attributePaths = {
            "instructor",
            "materials",
            "modules",
            "modules.contents"
    })
    Optional<CourseEntity> findDetailsById(UUID id);
}