package com.escapa.backend.application.course;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.entity.Course;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Detecta ciclos na grafo de pré-requisitos considerando rascunhos de cursos publicados.
 */
public final class CoursePrerequisiteCycles {

    private final CourseRepositoryPort courseRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;

    public CoursePrerequisiteCycles(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository) {
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    /** {@code true} se adicionar {@code candidate} como pré-requisito de {@code courseId} formar ciclo. */
    public boolean createsCycle(UUID courseId, UUID candidate) {
        return reaches(courseId, candidate, new HashSet<>(), Map.of());
    }

    /**
     * Garante que o conjunto que será publicado para {@code courseId} não forma ciclo
     * com os pré-requisitos efetivos (publicados ou em rascunho) dos demais cursos.
     */
    public void assertAcyclic(UUID courseId, List<UUID> prerequisiteIds) {
        final List<UUID> ids = prerequisiteIds == null ? List.of() : prerequisiteIds;
        final Map<UUID, List<UUID>> overrides = Map.of(courseId, List.copyOf(ids));
        for (final UUID prerequisiteId : ids) {
            if (courseId.equals(prerequisiteId) || reaches(courseId, prerequisiteId, new HashSet<>(), overrides)) {
                throw new IllegalArgumentException("O pré-requisito formaria um ciclo.");
            }
        }
    }

    private boolean reaches(UUID target, UUID current, Set<UUID> visited, Map<UUID, List<UUID>> overrides) {
        if (target.equals(current)) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        return effectivePrerequisiteIds(current, overrides).stream()
                .anyMatch(next -> reaches(target, next, visited, overrides));
    }

    private List<UUID> effectivePrerequisiteIds(UUID courseId, Map<UUID, List<UUID>> overrides) {
        if (overrides.containsKey(courseId)) {
            return overrides.get(courseId);
        }
        final Course course = courseRepository.findById(courseId).orElse(null);
        if (course != null && PublishedCourseDrafts.isPublished(course)) {
            final CoursePendingDraft draft = course.getPendingDraft();
            if (draft != null && draft.prerequisiteIds() != null) {
                return draft.prerequisiteIds();
            }
        }
        return prerequisiteRepository.findPrerequisiteCourseIds(courseId);
    }
}
