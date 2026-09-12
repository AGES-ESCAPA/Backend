package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.entity.Course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Fake em memória de {@link CourseRepositoryPort} para testes unitários.
 * Simula filtragem e paginação (US-01), busca de detalhes (US-04) e o
 * CRUD administrativo (US-05) sem banco de dados.
 */
final class InMemoryCourseRepositoryPort implements CourseRepositoryPort {

    private final List<CourseSummary> courses = new ArrayList<>();
    private final Map<UUID, CourseDetails> detailsById = new HashMap<>();
    private final Map<UUID, Course> coursesById = new HashMap<>();

    void addCourse(CourseSummary course) {
        courses.add(course);
    }

    void clear() {
        courses.clear();
    }

    void saveDetails(CourseDetails course) {
        detailsById.put(course.id(), course);
    }

    @Override
    public Course save(Course course) {
        if (course.getId() == null) {
            course.setId(UUID.randomUUID());
        }
        coursesById.put(course.getId(), course);
        return course;
    }

    @Override
    public Optional<Course> findById(UUID id) {
        return Optional.ofNullable(coursesById.get(id));
    }

    @Override
    public List<Course> findAll() {
        return List.copyOf(coursesById.values());
    }

    @Override
    public List<Course> searchByTitle(String title, UUID excludedCourseId) {
        final String normalized = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return coursesById.values().stream()
                .filter(c -> !c.getId().equals(excludedCourseId))
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
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

    @Override
    public Optional<CourseDetails> findDetailsById(UUID id) {
        return Optional.ofNullable(detailsById.get(id));
    }
}
