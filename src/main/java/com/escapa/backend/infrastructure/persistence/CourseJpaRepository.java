package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.escapa.backend.course.shared.entity.CourseEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.escapa.backend.course.shared.entity.CourseStatus;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {

    List<CourseEntity> findByTitleContainingIgnoreCaseAndIdNot(
            String title,
            UUID id
    );

    @Query(value = """
            SELECT c FROM CourseEntity c
            LEFT JOIN FETCH c.instructor
            WHERE c.status = :status
            AND LOWER(c.title) LIKE :titlePattern
            AND (:category = '' OR c.category = :category)
            AND (:level = '' OR c.level = :level)
            """,
            countQuery = """
            SELECT COUNT(c) FROM CourseEntity c
            WHERE c.status = :status
            AND LOWER(c.title) LIKE :titlePattern
            AND (:category = '' OR c.category = :category)
            AND (:level = '' OR c.level = :level)
            """)
    Page<CourseEntity> findPublishedCourses(
            @Param("status") CourseStatus status,
            @Param("titlePattern") String titlePattern,
            @Param("category") String category,
            @Param("level") String level,
            Pageable pageable
    );
}