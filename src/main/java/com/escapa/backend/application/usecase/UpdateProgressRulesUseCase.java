package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.PublishedCourseDrafts;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

import java.util.Objects;
import java.util.UUID;

public class UpdateProgressRulesUseCase {

    private final CourseRepositoryPort courseRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;

    public UpdateProgressRulesUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository) {
        this.courseRepository = courseRepository;
        this.changeLogRepository = changeLogRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public void execute(
            UUID courseId, boolean requireSequentialProgress, boolean enforceDeadlineBlock, User changedBy) {
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (PublishedCourseDrafts.isPublished(course)) {
            savePublishedDraft(course, requireSequentialProgress, enforceDeadlineBlock);
            return;
        }

        final boolean changed = !Objects.equals(requireSequentialProgress, course.getRequireSequentialProgress())
                || !Objects.equals(enforceDeadlineBlock, course.getEnforceDeadlineBlock());

        course.setRequireSequentialProgress(requireSequentialProgress);
        course.setEnforceDeadlineBlock(enforceDeadlineBlock);

        final Course saved = courseRepository.save(course);

        if (changed) {
            changeLogRepository.save(courseId, changedBy != null ? changedBy.getId() : null,
                    "Alteração nas regras de progressão.", saved.getMajorVersion(), saved.getMinorVersion());
        }
    }

    private void savePublishedDraft(Course course, boolean requireSequentialProgress, boolean enforceDeadlineBlock) {
        final CoursePendingDraft current = PublishedCourseDrafts.ensure(course, prerequisiteRepository);
        if (Objects.equals(current.requireSequentialProgress(), requireSequentialProgress)
                && Objects.equals(current.enforceDeadlineBlock(), enforceDeadlineBlock)) {
            return;
        }
        course.setPendingDraft(current.withRules(requireSequentialProgress, enforceDeadlineBlock));
        courseRepository.save(course);
    }
}
