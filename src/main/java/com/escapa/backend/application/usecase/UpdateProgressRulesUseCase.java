package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

import java.util.UUID;

public class UpdateProgressRulesUseCase {

    private final CourseRepositoryPort courseRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public UpdateProgressRulesUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        this.courseRepository = courseRepository;
        this.changeLogRepository = changeLogRepository;
    }

    public void execute(
            UUID courseId, boolean requireSequentialProgress, boolean enforceDeadlineBlock, User changedBy) {
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        final boolean changed = requireSequentialProgress != course.getRequireSequentialProgress()
                || enforceDeadlineBlock != course.getEnforceDeadlineBlock();

        course.setRequireSequentialProgress(requireSequentialProgress);
        course.setEnforceDeadlineBlock(enforceDeadlineBlock);
        if (changed && course.getStatus() == CourseStatus.PUBLISHED) {
            course.setMinorVersion(course.getMinorVersion() + 1);
        }

        final Course saved = courseRepository.save(course);

        if (changed) {
            changeLogRepository.save(courseId, changedBy != null ? changedBy.getId() : null,
                    "Alteração nas regras de progressão.", saved.getMajorVersion(), saved.getMinorVersion());
        }
    }
}
