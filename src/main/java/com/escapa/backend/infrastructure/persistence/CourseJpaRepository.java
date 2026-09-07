package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {

    List<CourseEntity> findByTitleContainingIgnoreCaseAndIdNot(
            String title,
            UUID id
    );
}