package com.escapa.backend.application.model;

import com.escapa.backend.domain.entity.Content;

import java.util.UUID;

/**
 * Aula junto do contexto necessario ao player: dados do modulo (cabecalho
 * "Modulo X - Aula Y") e a regra de prazo do curso, usada na checagem de acesso.
 */
public record LessonDetails(
        Content content,
        UUID courseId,
        String moduleTitle,
        Integer moduleOrder,
        boolean courseEnforcesDeadlineBlock,
        LessonSupplement supplement
) {
}
