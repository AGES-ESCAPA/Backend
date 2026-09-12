package com.escapa.backend.adapters.dto;

import java.util.UUID;

/**
 * DTO de resposta para cada curso na listagem pública.
 * O campo {@code instructor} contém o nome do instrutor (não o objeto completo).
 */
public record CourseCardResponse(
        UUID id,
        String title,
        String shortDescription,
        String category,
        String level,
        Integer durationTime,
        Integer lessonsCount,
        Double price,
        String thumbnailUrl,
        String instructor,
        Double ratingAverage,
        Integer reviewsCount
) {
}
