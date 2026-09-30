package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

public interface LessonProgressRepositoryPort {

    /** Ids das aulas do curso informado que o aluno ja concluiu. */
    List<UUID> findCompletedLessonIds(UUID userId, UUID courseId);
}
