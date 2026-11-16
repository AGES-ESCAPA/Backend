package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.PublishedCourseDrafts;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

import java.util.UUID;

public class RemoveCoursePrerequisiteUseCase {

    private final CourseRepositoryPort courseRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public RemoveCoursePrerequisiteUseCase(
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
        if (PublishedCourseDrafts.isPublished(course)) {
            final CoursePendingDraft draft = PublishedCourseDrafts.ensure(course, prerequisiteRepository);
            if (!draft.containsPrerequisite(prerequisiteCourseId)) {
                throw new IllegalArgumentException("Pré-requisito não encontrado.");
            }
            course.setPendingDraft(draft.withoutPrerequisite(prerequisiteCourseId));
            courseRepository.save(course);
            return;
        }
        if (!prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(courseId, prerequisiteCourseId)) {
            throw new IllegalArgumentException("Pré-requisito não encontrado.");
        }

        prerequisiteRepository.deleteByCourseIdAndPrerequisiteCourseId(courseId, prerequisiteCourseId);
        changeLogRepository.save(courseId, changedBy != null ? changedBy.getId() : null,
                "Pré-requisito removido.", course.getMajorVersion(), course.getMinorVersion());
    }
}
