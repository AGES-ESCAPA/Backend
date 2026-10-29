package com.escapa.backend.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LessonProgressRepositoryPort {

    /** Ids das aulas do curso informado que o aluno ja concluiu. */
    List<UUID> findCompletedLessonIds(UUID userId, UUID courseId);

    /** Marca a aula como concluida pelo aluno. Se ja estava concluida, mantem a data original. */
    void markCompleted(UUID userId, UUID lessonId, LocalDateTime completedAt);
}
