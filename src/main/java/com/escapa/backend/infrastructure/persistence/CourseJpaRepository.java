package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {

    List<CourseEntity> findByTitleContainingIgnoreCaseAndIdNot(
            String title,
            UUID id
    );

    @Query(value = """
            SELECT c FROM CourseEntity c
            LEFT JOIN FETCH c.instructor
            WHERE c.status = :status
            AND (:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
            AND (:category IS NULL OR c.category = :category)
            AND (:level IS NULL OR c.level = :level)
            """,
            countQuery = """
            SELECT COUNT(c) FROM CourseEntity c
            WHERE c.status = :status
            AND (:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
            AND (:category IS NULL OR c.category = :category)
            AND (:level IS NULL OR c.level = :level)
            """)
    Page<CourseEntity> findPublishedCourses(
            @Param("status") CourseStatus status,
            @Param("title") String title,
            @Param("category") String category,
            @Param("level") String level,
            Pageable pageable
    );
}