package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.LessonSupplement;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import com.escapa.backend.application.port.LessonRepositoryPort;
import com.escapa.backend.application.port.LessonSupplementRepositoryPort;
import com.escapa.backend.application.port.ModulePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;

/**
 * Abre uma aula para o aluno (US-11), validando o acesso: aula gratuita e
 * liberada a qualquer aluno identificado; as demais exigem matricula iniciada
 * e, se o curso bloqueia por prazo, ainda dentro da validade.
 * Com progressao sequencial ligada, recusa aulas ainda {@code LOCKED} (US-13).
 *
 * <p>Quando o acesso e permitido, registra o ultimo acesso do aluno ao curso
 * (US-35). Por gravar dados, o nome nao usa o prefixo {@code Get}: o caso de uso
 * precisa rodar em transacao de escrita (ver {@code UseCaseTransactionConfig}).
 * Aula gratuita de quem nao e matriculado nao registra nada.
 */
public class OpenStudentLessonUseCase {

    // Recargas e "tentar novamente" nao viram uma escrita cada: so grava se o ultimo acesso for mais antigo.
    private static final Duration LAST_ACCESS_MIN_INTERVAL = Duration.ofMinutes(1);

    private final LessonRepositoryPort lessonRepositoryPort;
    private final LessonSupplementRepositoryPort lessonSupplementRepositoryPort;
    private final EnrollmentRepositoryPort enrollmentRepositoryPort;
    private final EnrollmentAccessValidator enrollmentAccessValidator;
    private final SequentialLessonAccessValidator sequentialLessonAccessValidator;
    private final Clock clock;

    public OpenStudentLessonUseCase(
            LessonRepositoryPort lessonRepositoryPort,
            EnrollmentRepositoryPort enrollmentRepositoryPort,
            LessonSupplementRepositoryPort lessonSupplementRepositoryPort,
            CourseRepositoryPort courseRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort,
            LessonProgressRepositoryPort lessonProgressRepositoryPort,
            ModulePrerequisiteRepositoryPort modulePrerequisiteRepositoryPort,
            Clock clock
    ) {
        this.lessonRepositoryPort = lessonRepositoryPort;
        this.lessonSupplementRepositoryPort = lessonSupplementRepositoryPort;
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
        this.enrollmentAccessValidator = new EnrollmentAccessValidator(enrollmentRepositoryPort, clock);
        this.sequentialLessonAccessValidator = new SequentialLessonAccessValidator(
                courseRepositoryPort,
                moduleRepositoryPort,
                lessonProgressRepositoryPort,
                modulePrerequisiteRepositoryPort);
        this.clock = clock;
    }

    public LessonDetails execute(UUID userId, UUID courseId, UUID lessonId) {
        // 404 antes de 403: aula inexistente ou de outro curso nao revela nada sobre acesso.
        final LessonDetails lesson = lessonRepositoryPort.findByCourseIdAndId(courseId, lessonId)
                .orElseThrow(() -> new ContentNotFoundException(lessonId));

        final boolean isFree = Boolean.TRUE.equals(lesson.content().getIsFree());
        if (!isFree) {
            enrollmentAccessValidator.requireActiveEnrollment(
                    userId, lesson.courseId(), lesson.courseEnforcesDeadlineBlock(), lesson.content().getId());
            sequentialLessonAccessValidator.requireUnlocked(userId, courseId, lessonId);
        } else if (enrollmentRepositoryPort.findByUserIdAndCourseId(userId, courseId).isPresent()) {
            // Amostra gratuita nao exige matricula; quem ja esta matriculado nao fura a trilha.
            sequentialLessonAccessValidator.requireUnlocked(userId, courseId, lessonId);
        }

        final LessonSupplement supplement = lessonSupplementRepositoryPort.getSupplementsByLessonId(lessonId);

        // Acesso liberado: registra o ultimo acesso por ultimo, para a gravacao nao reter o lock da linha
        // durante as consultas acima. A porta ignora quem nao tem matricula nao revogada.
        enrollmentRepositoryPort.recordLastAccess(
                userId, lesson.courseId(), LocalDateTime.now(clock), LAST_ACCESS_MIN_INTERVAL);

        return new LessonDetails(
                lesson.content(),
                lesson.courseId(),
                lesson.moduleTitle(),
                lesson.moduleOrder(),
                lesson.courseEnforcesDeadlineBlock(),
                supplement
        );
    }
}
