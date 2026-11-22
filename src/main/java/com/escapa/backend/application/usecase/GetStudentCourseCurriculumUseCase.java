package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.course.PublishedCurriculum;
import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import com.escapa.backend.application.port.ModulePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;

/**
 * Monta a grade de aulas de um curso para o aluno (US-13): modulos, aulas e o
 * status de cada uma (COMPLETED/AVAILABLE/LOCKED). Reaproveita a mesma validacao
 * de acesso da API de conteudo da aula (US-11): sem matricula, so amostras
 * {@code isFree} ficam liberadas.
 */
public class GetStudentCourseCurriculumUseCase {

    private final CourseRepositoryPort courseRepositoryPort;
    private final ModuleRepositoryPort moduleRepositoryPort;
    private final LessonProgressRepositoryPort lessonProgressRepositoryPort;
    private final ModulePrerequisiteRepositoryPort modulePrerequisiteRepositoryPort;
    private final EnrollmentRepositoryPort enrollmentRepositoryPort;
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
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
        this.enrollmentAccessValidator = new EnrollmentAccessValidator(enrollmentRepositoryPort, clock);
        this.lessonStatusCalculator = new LessonStatusCalculator();
    }

    public StudentCourseCurriculum execute(UUID userId, UUID courseId) {
        final Course course = courseRepositoryPort.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        final List<Module> modules = PublishedCurriculum.forStudents(
                moduleRepositoryPort.findByCourseId(courseId), course.getStatus());
        final boolean enrolled = enrollmentRepositoryPort.findByUserIdAndCourseId(userId, courseId).isPresent();

        if (enrolled) {
            enrollmentAccessValidator.requireActiveEnrollment(
                    userId, courseId, Boolean.TRUE.equals(course.getEnforceDeadlineBlock()), courseId);
        } else if (!hasFreeLesson(modules)) {
            throw new LessonAccessDeniedException(Reason.NOT_ENROLLED, courseId);
        }

        final List<UUID> completedLessonIds = enrolled
                ? lessonProgressRepositoryPort.findCompletedLessonIds(userId, courseId)
                : List.of();

        return lessonStatusCalculator.calculate(
                courseId,
                modules,
                completedLessonIds,
                modulePrerequisiteRepositoryPort::findPrerequisiteModuleIds,
                Boolean.TRUE.equals(course.getRequireSequentialProgress()),
                enrolled);
    }

    private static boolean hasFreeLesson(List<Module> modules) {
        return modules.stream()
                .flatMap(module -> module.getContents().stream())
                .anyMatch(lesson -> Boolean.TRUE.equals(lesson.getIsFree()));
    }
}
