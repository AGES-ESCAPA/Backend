package com.escapa.backend.application.dto;

import java.util.UUID;

/**
 * Resumo de um curso para listagem pública.
 * Carrega apenas os campos necessários para o card da vitrine.
 */
public record CourseSummary(
        UUID id,
        String title,
        String shortDescription,
        String category,
        String level,
        Integer durationTime,
        Integer lessonsCount,
        Double price,
        String thumbnailUrl,
        String instructorName,
        Double ratingAverage,
        Integer reviewsCount
) {
}
