package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import com.escapa.backend.application.port.LessonRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Marca uma aula como concluida pelo aluno (US-15). Aplica a mesma regra de
 * acesso da API de conteudo da aula (US-11) e e idempotente: concluir de novo
 * uma aula ja concluida nao altera a data original.
 */
public class CompleteStudentLessonUseCase {

    private final LessonRepositoryPort lessonRepositoryPort;
    private final LessonProgressRepositoryPort lessonProgressRepositoryPort;
    private final EnrollmentAccessValidator enrollmentAccessValidator;
    private final Clock clock;

    public CompleteStudentLessonUseCase(
            LessonRepositoryPort lessonRepositoryPort,
            LessonProgressRepositoryPort lessonProgressRepositoryPort,
            EnrollmentRepositoryPort enrollmentRepositoryPort,
            Clock clock
    ) {
        this.lessonRepositoryPort = lessonRepositoryPort;
        this.lessonProgressRepositoryPort = lessonProgressRepositoryPort;
        this.enrollmentAccessValidator = new EnrollmentAccessValidator(enrollmentRepositoryPort, clock);
        this.clock = clock;
    }

    public void execute(UUID userId, UUID courseId, UUID lessonId) {
        // 404 antes de 403: aula inexistente ou de outro curso nao revela nada sobre acesso.
        final LessonDetails lesson = lessonRepositoryPort.findByCourseIdAndId(courseId, lessonId)
                .orElseThrow(() -> new ContentNotFoundException(lessonId));

        if (!Boolean.TRUE.equals(lesson.content().getIsFree())) {
            enrollmentAccessValidator.requireActiveEnrollment(
                    userId, lesson.courseId(), lesson.courseEnforcesDeadlineBlock(), lesson.content().getId());
        }

        lessonProgressRepositoryPort.markCompleted(userId, lessonId, LocalDateTime.now(clock));
    }
}
