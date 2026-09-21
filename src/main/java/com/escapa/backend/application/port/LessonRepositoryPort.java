package com.escapa.backend.application.port;

import com.escapa.backend.application.model.LessonDetails;

import java.util.Optional;
import java.util.UUID;

public interface LessonRepositoryPort {

    /**
     * Aula com os dados do seu modulo e do curso.
     *
     * @return vazio se a aula nao existir ou seu modulo nao pertencer a {@code courseId}
     */
    Optional<LessonDetails> findByCourseIdAndId(UUID courseId, UUID lessonId);
}
