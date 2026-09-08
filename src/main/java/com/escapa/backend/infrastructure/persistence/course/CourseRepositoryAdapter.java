package com.escapa.backend.infrastructure.persistence.course;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Implementação concreta de {@link CourseRepositoryPort} usando Spring Data JPA.
 * Traduz {@code Page<CourseEntity>} do Spring para {@code PageResult<CourseSummary>} do domínio.
 */
public class CourseRepositoryAdapter implements CourseRepositoryPort {

    private final CourseJpaRepository jpaRepository;

    public CourseRepositoryAdapter(CourseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PageResult<CourseSummary> findPublished(
            String title, String category, String level, int page, int size) {
        final Pageable pageable = PageRequest.of(page, size);
        final String safeTitle = title != null ? title : "";
        final String safeCategory = category != null ? category : "";
        final String safeLevel = level != null ? level : "";

        final Page<CourseEntity> jpaPage = jpaRepository.findPublishedCourses(
                CourseStatus.PUBLISHED, safeTitle, safeCategory, safeLevel, pageable);
        final List<CourseSummary> content = jpaPage.getContent().stream()
                .map(CourseMapper::toSummary)
                .toList();
        return new PageResult<>(
                content, jpaPage.getNumber(), jpaPage.getSize(),
                jpaPage.getTotalElements(), jpaPage.getTotalPages());
    }
}
