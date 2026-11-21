package com.escapa.backend.application.usecase;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.course.PublishedCurriculum;
import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import com.escapa.backend.application.port.ModulePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.content.LessonStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;

/**
 * Recusa abrir ou concluir aula ainda {@code LOCKED} quando o curso exige
 * progressao sequencial — a mesma regra da grade (US-13) aplicada na API de
 * conteudo (US-11) e de conclusao (US-15).
 */
public class SequentialLessonAccessValidator {

    private final CourseRepositoryPort courseRepositoryPort;
    private final ModuleRepositoryPort moduleRepositoryPort;
    private final LessonProgressRepositoryPort lessonProgressRepositoryPort;
    private final ModulePrerequisiteRepositoryPort modulePrerequisiteRepositoryPort;
    private final LessonStatusCalculator lessonStatusCalculator = new LessonStatusCalculator();

    public SequentialLessonAccessValidator(
            CourseRepositoryPort courseRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort,
            LessonProgressRepositoryPort lessonProgressRepositoryPort,
            ModulePrerequisiteRepositoryPort modulePrerequisiteRepositoryPort
    ) {
        this.courseRepositoryPort = courseRepositoryPort;
        this.moduleRepositoryPort = moduleRepositoryPort;
        this.lessonProgressRepositoryPort = lessonProgressRepositoryPort;
        this.modulePrerequisiteRepositoryPort = modulePrerequisiteRepositoryPort;
    }

    public void requireUnlocked(UUID userId, UUID courseId, UUID lessonId) {
        final Course course = courseRepositoryPort.findById(courseId).orElse(null);
        if (course == null || !Boolean.TRUE.equals(course.getRequireSequentialProgress())) {
            return;
        }

        final List<Module> modules = PublishedCurriculum.forStudents(
                moduleRepositoryPort.findByCourseId(courseId), course.getStatus());
        final List<UUID> completedLessonIds =
                lessonProgressRepositoryPort.findCompletedLessonIds(userId, courseId);

        final StudentCourseCurriculum curriculum = lessonStatusCalculator.calculate(
                courseId,
                modules,
                completedLessonIds,
                modulePrerequisiteRepositoryPort::findPrerequisiteModuleIds,
                true);

        final LessonStatus status = curriculum.modules().stream()
                .flatMap(module -> module.lessons().stream())
                .filter(lesson -> lesson.lessonId().equals(lessonId))
                .map(StudentCourseCurriculum.Lesson::status)
                .findFirst()
                .orElse(null);

        if (status == LessonStatus.LOCKED) {
            throw new LessonAccessDeniedException(Reason.SEQUENTIAL_PROGRESS_LOCKED, lessonId);
        }
    }
}
