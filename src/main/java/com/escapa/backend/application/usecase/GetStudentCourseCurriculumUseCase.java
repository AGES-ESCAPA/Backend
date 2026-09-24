package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import com.escapa.backend.application.port.ModulePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;

/**
 * Monta a grade de aulas de um curso para o aluno (US-13): modulos, aulas e o
 * status de cada uma (COMPLETED/AVAILABLE/LOCKED). Reaproveita a mesma validacao
 * de acesso da API de conteudo da aula (US-11).
 */
public class GetStudentCourseCurriculumUseCase {

    private final CourseRepositoryPort courseRepositoryPort;
    private final ModuleRepositoryPort moduleRepositoryPort;
    private final LessonProgressRepositoryPort lessonProgressRepositoryPort;
    private final ModulePrerequisiteRepositoryPort modulePrerequisiteRepositoryPort;
    private final EnrollmentAccessValidator enrollmentAccessValidator;
    private final LessonStatusCalculator lessonStatusCalculator;

    public GetStudentCourseCurriculumUseCase(
            CourseRepositoryPort courseRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort,
            LessonProgressRepositoryPort lessonProgressRepositoryPort,
            ModulePrerequisiteRepositoryPort modulePrerequisiteRepositoryPort,
            EnrollmentRepositoryPort enrollmentRepositoryPort,
            Clock clock
    ) {
        this.courseRepositoryPort = courseRepositoryPort;
        this.moduleRepositoryPort = moduleRepositoryPort;
        this.lessonProgressRepositoryPort = lessonProgressRepositoryPort;
        this.modulePrerequisiteRepositoryPort = modulePrerequisiteRepositoryPort;
        this.enrollmentAccessValidator = new EnrollmentAccessValidator(enrollmentRepositoryPort, clock);
        this.lessonStatusCalculator = new LessonStatusCalculator();
    }

    public StudentCourseCurriculum execute(UUID userId, UUID courseId) {
        final Course course = courseRepositoryPort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        enrollmentAccessValidator.requireActiveEnrollment(
                userId, courseId, Boolean.TRUE.equals(course.getEnforceDeadlineBlock()), courseId);

        final List<Module> modules = moduleRepositoryPort.findByCourseId(courseId);
        final List<UUID> completedLessonIds = lessonProgressRepositoryPort.findCompletedLessonIds(userId, courseId);

        return lessonStatusCalculator.calculate(
                courseId,
                modules,
                completedLessonIds,
                modulePrerequisiteRepositoryPort::findPrerequisiteModuleIds,
                Boolean.TRUE.equals(course.getRequireSequentialProgress()));
    }
}