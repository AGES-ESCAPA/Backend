package com.escapa.backend.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entrada do histórico de alterações de um curso (US-09), já com o nome de quem
 * alterou resolvido ("Sistema" quando a alteração não tem autor).
 */
public record ChangeLogEntry(
        UUID id,
        String description,
        String changedByName,
        int majorVersion,
        int minorVersion,
        LocalDateTime createdAt
) {
}
