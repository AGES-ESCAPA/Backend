package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

/**
 * Aplica no currículo publicado o que o construtor guardou desde a última publicação.
 */
public interface CurriculumReleasePort {

    List<ReleasedLessonChange> release(UUID courseId);

    /**
     * Aula já visível cujo conteúdo publicado mudou. Aula nova não entra:
     * ninguém a tinha concluído.
     */
    record ReleasedLessonChange(UUID lessonId, String title) {
    }
}
