package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.PublishedCourseDrafts;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AddCoursePrerequisiteUseCase {

    private final CourseRepositoryPort courseRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public AddCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.changeLogRepository = changeLogRepository;
    }

    public void execute(UUID courseId, UUID prerequisiteCourseId, User changedBy) {
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        courseRepository.findById(prerequisiteCourseId)
                .orElseThrow(() -> new CourseNotFoundException(prerequisiteCourseId));

        if (courseId.equals(prerequisiteCourseId)) {
            throw new IllegalArgumentException("Um curso não pode ser pré-requisito dele mesmo.");
        }
        if (createsCycle(courseId, prerequisiteCourseId, new HashSet<>())) {
            throw new IllegalArgumentException("O pré-requisito formaria um ciclo.");
        }

        if (PublishedCourseDrafts.isPublished(course)) {
            final CoursePendingDraft draft = PublishedCourseDrafts.ensure(course, prerequisiteRepository);
            if (draft.containsPrerequisite(prerequisiteCourseId)) {
                throw new IllegalArgumentException("Pré-requisito já cadastrado.");
            }
            course.setPendingDraft(draft.withPrerequisite(prerequisiteCourseId));
            courseRepository.save(course);
            return;
        }

        if (prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(courseId, prerequisiteCourseId)) {
            throw new IllegalArgumentException("Pré-requisito já cadastrado.");
        }

        prerequisiteRepository.save(courseId, prerequisiteCourseId);
        changeLogRepository.save(courseId, changedBy != null ? changedBy.getId() : null,
                "Pré-requisito adicionado.", course.getMajorVersion(), course.getMinorVersion());
    }

    private boolean createsCycle(UUID courseId, UUID candidate, Set<UUID> visited) {
        if (courseId.equals(candidate)) {
            return true;
        }
        if (!visited.add(candidate)) {
            return false;
        }
        return prerequisiteRepository.findPrerequisiteCourseIds(candidate).stream()
                .anyMatch(next -> createsCycle(courseId, next, visited));
    }
}
