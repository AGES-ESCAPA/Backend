package com.escapa.backend.application.port;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface CourseReviewRepositoryPort {

    /** Grava a nota do aluno no curso; se ja houver uma, substitui a nota anterior. */
    void saveRating(UUID userId, UUID courseId, int rating, LocalDateTime ratedAt);

    /** Notas que o aluno deu aos cursos informados, por id do curso. Cursos sem nota ficam de fora. */
    Map<UUID, Integer> findRatingsByUser(UUID userId, Collection<UUID> courseIds);
}
