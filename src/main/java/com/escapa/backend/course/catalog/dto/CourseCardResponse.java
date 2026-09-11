package com.escapa.backend.course.catalog.dto;

import java.util.UUID;

/**
 * Card da vitrine pública (US-01). Preenchido direto pela consulta JPQL do
 * {@code CourseCatalogRepository} via {@code SELECT new}, sem carregar a entidade.
 *
 * <p>A ordem e os tipos dos componentes precisam bater com a projeção da consulta.
 * {@code instructor} é o nome do instrutor, não o objeto.
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
