package com.escapa.backend.application.model;

import java.util.UUID;

/**
 * Curso que a empresa possui (não revogado), com o prazo em dias que vale para cada matrícula.
 *
 * @param deadline dias de acesso a partir da matrícula; nulo quando o curso não expira
 */
public record AcquiredCourse(UUID courseId, Integer deadline) {
}
