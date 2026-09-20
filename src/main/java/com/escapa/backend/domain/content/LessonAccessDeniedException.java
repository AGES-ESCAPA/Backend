package com.escapa.backend.domain.content;

import java.util.UUID;

/** O aluno identificado nao pode acessar a aula pedida (matricula ausente, nao iniciada ou expirada). */
public class LessonAccessDeniedException extends RuntimeException {

    public enum Reason {
        NOT_ENROLLED("user is not enrolled in the course"),
        ENROLLMENT_NOT_STARTED("enrollment has not started yet"),
        ACCESS_EXPIRED("access period has expired");

        private final String description;

        Reason(String description) {
            this.description = description;
        }
    }

    private final Reason reason;

    public LessonAccessDeniedException(Reason reason, UUID lessonId) {
        super("Access denied to lesson " + lessonId + ": " + reason.description);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
