package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.LessonSupplement;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.LessonRepositoryPort;
import com.escapa.backend.application.port.LessonSupplementRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Carrega uma aula para o aluno (US-11), validando o acesso: aula gratuita e
 * liberada a qualquer aluno identificado; as demais exigem matricula iniciada e,
 * se o curso bloqueia por prazo, ainda dentro da validade.
 * O bloqueio por ordem obrigatoria depende do progresso por aula (US-13).
 */
public class GetStudentLessonUseCase {

    private final LessonRepositoryPort lessonRepositoryPort;
    private final EnrollmentRepositoryPort enrollmentRepositoryPort;
    private final LessonSupplementRepositoryPort lessonSupplementRepositoryPort;
    private final Clock clock;

    public GetStudentLessonUseCase(
            LessonRepositoryPort lessonRepositoryPort,
            EnrollmentRepositoryPort enrollmentRepositoryPort,
            LessonSupplementRepositoryPort lessonSupplementRepositoryPort,
            Clock clock
    ) {
        this.lessonRepositoryPort = lessonRepositoryPort;
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
        this.lessonSupplementRepositoryPort = lessonSupplementRepositoryPort;
        this.clock = clock;
    }

    public LessonDetails execute(UUID userId, UUID courseId, UUID lessonId) {
        // 404 antes de 403: aula inexistente ou de outro curso nao revela nada sobre acesso.
        final LessonDetails lesson = lessonRepositoryPort.findByCourseIdAndId(courseId, lessonId)
                .orElseThrow(() -> new ContentNotFoundException(lessonId));

        if (!Boolean.TRUE.equals(lesson.content().getIsFree())) {
            requireActiveEnrollment(userId, lesson);
        }

        final LessonSupplement supplement = lessonSupplementRepositoryPort.getSupplementsByLessonId(lessonId);

        return new LessonDetails(
                lesson.content(),
                lesson.courseId(),
                lesson.moduleTitle(),
                lesson.moduleOrder(),
                lesson.courseEnforcesDeadlineBlock(),
                supplement
        );
    }

    private void requireActiveEnrollment(UUID userId, LessonDetails lesson) {
        final UUID lessonId = lesson.content().getId();
        final Enrollment enrollment = enrollmentRepositoryPort.findByUserIdAndCourseId(userId, lesson.courseId())
                .orElseThrow(() -> new LessonAccessDeniedException(Reason.NOT_ENROLLED, lessonId));
        final LocalDate today = LocalDate.now(clock);

        if (enrollment.startDate() != null && enrollment.startDate().isAfter(today)) {
            throw new LessonAccessDeniedException(Reason.ENROLLMENT_NOT_STARTED, lessonId);
        }
        // O ultimo dia de validade ainda da acesso (mesma regra de findActiveByCourseId).
        if (lesson.courseEnforcesDeadlineBlock()
                && enrollment.expirationDate() != null
                && enrollment.expirationDate().isBefore(today)) {
            throw new LessonAccessDeniedException(Reason.ACCESS_EXPIRED, lessonId);
        }
    }
}
