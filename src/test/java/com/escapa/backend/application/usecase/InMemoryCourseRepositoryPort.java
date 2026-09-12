package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Fake em memória de {@link CourseRepositoryPort} para testes unitários.
 * Simula filtragem e paginação sem banco de dados.
 */
final class InMemoryCourseRepositoryPort implements CourseRepositoryPort {

    private final List<CourseSummary> courses = new ArrayList<>();

    void addCourse(CourseSummary course) {
        courses.add(course);
    }

    void clear() {
        courses.clear();
    }

    @Override
    public Optional<CourseEntity> findById(UUID id) {
        throw new UnsupportedOperationException("findById não é usado neste fake.");
    }

    @Override
    public List<CourseEntity> searchByTitle(String query, UUID excludedCourseId) {
        throw new UnsupportedOperationException("searchByTitle não é usado neste fake.");
    }

    @Override
    public CourseEntity save(CourseEntity course) {
        throw new UnsupportedOperationException("save não é usado neste fake.");
    }

    @Override
    public PageResult<CourseSummary> findPublished(
            String title, String category, String level, int page, int size) {
        final List<CourseSummary> filtered = courses.stream()
                .filter(c -> title == null
                        || c.title().toLowerCase(Locale.ROOT).contains(title.toLowerCase(Locale.ROOT)))
                .filter(c -> category == null || category.equals(c.category()))
                .filter(c -> level == null || level.equals(c.level()))
                .toList();

        final int total = filtered.size();
        final int start = page * size;
        final int end = Math.min(start + size, total);
        final List<CourseSummary> content = start < total
                ? filtered.subList(start, end) : List.of();
        final int totalPages = total == 0 ? 0 : (total + size - 1) / size;

        return new PageResult<>(content, page, size, total, totalPages);
    }
}
