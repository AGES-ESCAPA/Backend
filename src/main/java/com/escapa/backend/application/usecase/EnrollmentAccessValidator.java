package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Valida se um aluno tem acesso a um curso: matricula iniciada e, quando o
 * curso bloqueia por prazo, ainda dentro da validade. Compartilhada pela
 * API de conteudo da aula (US-11) e pela API de grade de aulas (US-13).
 */
public class EnrollmentAccessValidator {

    private final EnrollmentRepositoryPort enrollmentRepositoryPort;
    private final Clock clock;

    public EnrollmentAccessValidator(EnrollmentRepositoryPort enrollmentRepositoryPort, Clock clock) {
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
        this.clock = clock;
    }

    public void requireActiveEnrollment(
            UUID userId,
            UUID courseId,
            boolean courseEnforcesDeadlineBlock,
            UUID resourceId
    ) {
        final Enrollment enrollment = enrollmentRepositoryPort.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new LessonAccessDeniedException(Reason.NOT_ENROLLED, resourceId));
        final LocalDate today = LocalDate.now(clock);

        if (enrollment.startDate() != null && enrollment.startDate().isAfter(today)) {
            throw new LessonAccessDeniedException(Reason.ENROLLMENT_NOT_STARTED, resourceId);
        }
        // O ultimo dia de validade ainda da acesso (mesma regra de findActiveByCourseId).
        if (courseEnforcesDeadlineBlock
                && enrollment.expirationDate() != null
                && enrollment.expirationDate().isBefore(today)) {
            throw new LessonAccessDeniedException(Reason.ACCESS_EXPIRED, resourceId);
        }
    }
}
