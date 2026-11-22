package com.escapa.backend.application.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.dto.PublishedCourseFilters;
import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;

/**
 * Fake em memória de {@link CourseRepositoryPort} para testes unitários.
 * Simula filtragem e paginação (US-01), busca de detalhes (US-04) e o
 * CRUD administrativo (US-05) sem banco de dados.
 * Grava e devolve cópias: alterar o curso lido não muda o que está salvo até {@link #save}.
 */
public final class InMemoryCourseRepositoryPort implements CourseRepositoryPort {

    private final List<CourseSummary> courses = new ArrayList<>();
    private final Map<UUID, CourseDetails> detailsById = new HashMap<>();
    private final Map<UUID, Course> coursesById = new HashMap<>();
    private final List<Course> savedSnapshots = new ArrayList<>();
    private int saveCount;

    public void addCourse(CourseSummary course) {
        courses.add(course);
    }

    public void clear() {
        courses.clear();
    }

    public void saveDetails(CourseDetails course) {
        detailsById.put(course.id(), course);
    }

    @Override
    public Course save(Course course) {
        saveCount++;
        if (course.getId() == null) {
            course.setId(UUID.randomUUID());
        }
        final Course stored = copyOf(course);
        coursesById.put(stored.getId(), stored);
        savedSnapshots.add(copyOf(stored));
        return copyOf(stored);
    }

    public int saveCount() {
        return saveCount;
    }

    public List<Course> savedSnapshots() {
        return savedSnapshots.stream().map(InMemoryCourseRepositoryPort::copyOf).toList();
    }

    @Override
    public Optional<Course> findById(UUID id) {
        final Course stored = coursesById.get(id);
        return stored == null ? Optional.empty() : Optional.of(copyOf(stored));
    }

    @Override
    public boolean existsById(UUID id) {
        return coursesById.containsKey(id);
    }

    @Override
    public List<Course> findAllExcludingStatus(CourseStatus excludedStatus) {
        return coursesById.values().stream()
                .filter(course -> course.getStatus() != excludedStatus)
                .map(InMemoryCourseRepositoryPort::copyOf)
                .toList();
    }

    @Override
    public List<Course> searchByTitle(String title, UUID excludedCourseId) {
        final String normalized = title == null ? "" : title.toLowerCase(Locale.ROOT);
        return coursesById.values().stream()
                .filter(c -> !c.getId().equals(excludedCourseId))
                .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(normalized))
                .map(InMemoryCourseRepositoryPort::copyOf)
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
    public PublishedCourseFilters findPublishedFilters() {
        final List<String> categories = courses.stream()
                .map(CourseSummary::category)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        final List<String> levels = courses.stream()
                .map(CourseSummary::level)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return new PublishedCourseFilters(categories, levels);
    }

    @Override
    public List<String> findDistinctCategories() {
        return coursesById.values().stream()
                .map(Course::getCategory)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public Optional<CourseDetails> findDetailsById(UUID id) {
        return Optional.ofNullable(detailsById.get(id));
    }

    private static Course copyOf(Course source) {
        final Course copy = new Course();
        copy.setId(source.getId());
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setShortDescription(source.getShortDescription());
        copy.setThumbnailUrl(source.getThumbnailUrl());
        copy.setTeaserVideoUrl(source.getTeaserVideoUrl());
        copy.setStatus(source.getStatus());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setInstructor(source.getInstructor());
        copy.setCategory(source.getCategory());
        copy.setLevel(source.getLevel());
        copy.setDurationTime(source.getDurationTime());
        copy.setDeadline(source.getDeadline());
        copy.setPrice(source.getPrice());
        copy.setLearningObjectives(source.getLearningObjectives() == null
                ? new ArrayList<>() : new ArrayList<>(source.getLearningObjectives()));
        copy.setRequireSequentialProgress(source.getRequireSequentialProgress());
        copy.setEnforceDeadlineBlock(source.getEnforceDeadlineBlock());
        copy.setMinQuizPassPercentage(source.getMinQuizPassPercentage());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setMajorVersion(source.getMajorVersion());
        copy.setMinorVersion(source.getMinorVersion());
        copy.setPendingDraft(source.getPendingDraft());
        copy.setModules(copyModules(source.getModules(), copy));
        return copy;
    }

    private static List<Module> copyModules(List<Module> modules, Course course) {
        final List<Module> copies = new ArrayList<>();
        if (modules == null) {
            return copies;
        }
        for (final Module module : modules) {
            final Module copy = new Module();
            copy.setId(module.getId());
            copy.setCourseId(module.getCourseId() != null ? module.getCourseId() : course.getId());
            copy.setCourse(course);
            copy.setTitle(module.getTitle());
            copy.setOrder(module.getOrder());
            copy.setPublishedOrder(module.getPublishedOrder());
            copy.setPendingTitle(module.getPendingTitle());
            copy.setPendingDeletion(module.isPendingDeletion());
            copy.setCoursePublished(module.isCoursePublished());
            copy.setContents(copyContents(module.getContents()));
            copies.add(copy);
        }
        return copies;
    }

    private static List<Content> copyContents(List<Content> contents) {
        final List<Content> copies = new ArrayList<>();
        if (contents == null) {
            return copies;
        }
        for (final Content content : contents) {
            copies.add(content.copy());
        }
        return copies;
    }
}
