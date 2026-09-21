package com.escapa.backend.application.model;

import com.escapa.backend.application.dto.ChangeLogEntry;

import java.util.List;
import java.util.UUID;

/**
 * Regras de progressão de um curso (US-09): se exige avanço linear, se o prazo de
 * acesso bloqueia, seus pré-requisitos e as entradas mais recentes do histórico
 * de alterações.
 */
public record CourseRules(
        Boolean requireSequentialProgress,
        Boolean enforceDeadlineBlock,
        String version,
        List<Prerequisite> prerequisites,
        List<ChangeLogEntry> recentChangeLog
) {

    public record Prerequisite(UUID courseId, String courseTitle) {
    }
}
