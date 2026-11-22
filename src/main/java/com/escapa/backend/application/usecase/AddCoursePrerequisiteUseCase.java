package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.application.course.CoursePrerequisiteCycles;
import com.escapa.backend.application.course.PublishedCourseDrafts;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

public class AddCoursePrerequisiteUseCase {

    private final CourseRepositoryPort courseRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;
    private final CoursePrerequisiteCycles prerequisiteCycles;

    public AddCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.changeLogRepository = changeLogRepository;
        this.prerequisiteCycles = new CoursePrerequisiteCycles(courseRepository, prerequisiteRepository);
    }

    public void execute(UUID courseId, UUID prerequisiteCourseId, User changedBy) {
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        courseRepository.findById(prerequisiteCourseId)
                .orElseThrow(() -> new CourseNotFoundException(prerequisiteCourseId));

        if (courseId.equals(prerequisiteCourseId)) {
            throw new IllegalArgumentException("Um curso não pode ser pré-requisito dele mesmo.");
        }
        if (prerequisiteCycles.createsCycle(courseId, prerequisiteCourseId)) {
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
}
