package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.model.CourseRules;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GetCourseRulesUseCase {

    private static final int RECENT_CHANGE_LOG_LIMIT = 10;

    private final CourseRepositoryPort courseRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public GetCourseRulesUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.changeLogRepository = changeLogRepository;
    }

    public CourseRules execute(UUID courseId) {
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        final List<CourseRules.Prerequisite> prerequisites = prerequisiteRepository
                .findPrerequisiteCourseIds(courseId).stream()
                .map(this::toPrerequisite)
                .filter(Objects::nonNull)
                .toList();

        final List<ChangeLogEntry> changeLog =
                changeLogRepository.findRecentByCourseId(courseId, RECENT_CHANGE_LOG_LIMIT);

        return new CourseRules(
                course.getRequireSequentialProgress(),
                course.getEnforceDeadlineBlock(),
                course.getMajorVersion() + "." + course.getMinorVersion(),
                prerequisites,
                changeLog);
    }

    private CourseRules.Prerequisite toPrerequisite(UUID prerequisiteCourseId) {
        return courseRepository.findById(prerequisiteCourseId)
                .map(course -> new CourseRules.Prerequisite(course.getId(), course.getTitle()))
                .orElse(null);
    }
}
