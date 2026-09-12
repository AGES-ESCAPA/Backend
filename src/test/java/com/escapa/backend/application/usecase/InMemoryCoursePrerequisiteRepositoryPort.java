package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fake em memória de {@link CoursePrerequisiteRepositoryPort} para testes unitários.
 */
final class InMemoryCoursePrerequisiteRepositoryPort implements CoursePrerequisiteRepositoryPort {

    private record Link(UUID courseId, UUID prerequisiteCourseId) {
    }

    private final List<Link> links = new ArrayList<>();

    @Override
    public List<UUID> findPrerequisiteCourseIds(UUID courseId) {
        return links.stream()
                .filter(link -> link.courseId().equals(courseId))
                .map(Link::prerequisiteCourseId)
                .toList();
    }

    @Override
    public boolean existsByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId) {
        return links.stream().anyMatch(
                link -> link.courseId().equals(courseId) && link.prerequisiteCourseId().equals(prerequisiteCourseId));
    }

    @Override
    public void save(UUID courseId, UUID prerequisiteCourseId) {
        links.add(new Link(courseId, prerequisiteCourseId));
    }

    @Override
    public void deleteByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId) {
        links.removeIf(
                link -> link.courseId().equals(courseId) && link.prerequisiteCourseId().equals(prerequisiteCourseId));
    }
}
