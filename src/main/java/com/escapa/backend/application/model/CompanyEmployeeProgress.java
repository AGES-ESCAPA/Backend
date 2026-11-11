package com.escapa.backend.application.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma linha do relatório de progresso: um colaborador em um curso da empresa (US-35).
 *
 * @param progress    percentual de 0 a 100; matrícula sem progresso registrado conta como 0
 * @param lastAccessAt nulo quando o colaborador nunca abriu uma aula do curso
 */
public record CompanyEmployeeProgress(
        UUID userId,
        String name,
        String avatarUrl,
        UUID courseId,
        String courseTitle,
        int progress,
        LocalDateTime lastAccessAt,
        boolean certificateIssued
) {
}
