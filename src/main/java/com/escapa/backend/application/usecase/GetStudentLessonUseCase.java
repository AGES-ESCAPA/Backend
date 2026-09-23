package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.LessonRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;

import java.time.Clock;
import java.util.UUID;

/**
 * Carrega uma aula para o aluno (US-11), validando o acesso: aula gratuita e
 * liberada a qualquer aluno identificado; as demais exigem matricula iniciada e,
 * se o curso bloqueia por prazo, ainda dentro da validade.
 * O bloqueio por ordem obrigatoria depende do progresso por aula (US-13).
 */
public class GetStudentLessonUseCase {

    private final LessonRepositoryPort lessonRepositoryPort;
    private final EnrollmentAccessValidator enrollmentAccessValidator;

    public GetStudentLessonUseCase(
            LessonRepositoryPort lessonRepositoryPort,
            EnrollmentRepositoryPort enrollmentRepositoryPort,
            Clock clock
    ) {
        this.lessonRepositoryPort = lessonRepositoryPort;
        this.enrollmentAccessValidator = new EnrollmentAccessValidator(enrollmentRepositoryPort, clock);
    }

    public LessonDetails execute(UUID userId, UUID courseId, UUID lessonId) {
        // 404 antes de 403: aula inexistente ou de outro curso nao revela nada sobre acesso.
        final LessonDetails lesson = lessonRepositoryPort.findByCourseIdAndId(courseId, lessonId)
                .orElseThrow(() -> new ContentNotFoundException(lessonId));

        if (!Boolean.TRUE.equals(lesson.content().getIsFree())) {
            enrollmentAccessValidator.requireActiveEnrollment(
                    userId, lesson.courseId(), lesson.courseEnforcesDeadlineBlock(), lesson.content().getId());
        }
        return lesson;
    }
}
