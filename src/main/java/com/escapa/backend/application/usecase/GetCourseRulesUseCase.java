package com.escapa.backend.application.usecase;

import com.escapa.backend.adapters.dto.CourseRulesResponse;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CoursePrerequisiteEntity;

import java.util.List;
import java.util.UUID;

public class GetCourseRulesUseCase {

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

    public CourseRulesResponse execute(UUID courseId) {

        final CourseEntity course = courseRepository.findById(courseId)
            .orElseThrow(() ->
                    new IllegalArgumentException("Curso não encontrado.")
            );

        final List<CoursePrerequisiteEntity> prerequisites =
            prerequisiteRepository.findByCourseId(courseId);

        final List<CourseChangeLogEntity> changeLogs =
            changeLogRepository.findByCourseId(courseId);

    return new CourseRulesResponse(
            course.getRequireSequentialProgress(),
            course.getEnforceDeadlineBlock(),
            course.getMajorVersion() + "." + course.getMinorVersion(),

            prerequisites.stream()
                    .map(p -> new CourseRulesResponse.PrerequisiteResponse(
                            p.getPrerequisiteCourse().getId().toString(),
                            p.getPrerequisiteCourse().getTitle()
                    ))
                    .toList(),

            changeLogs.stream()
                    .map(log -> new CourseRulesResponse.ChangeLogResponse(
                            log.getId(),
                            log.getDescription(),
                            log.getChangedBy() == null
                                    ? "Sistema"
                                    : log.getChangedBy().getName(),
                            log.getCreatedAt().toString()
                    ))
                    .toList()
    );
}
}