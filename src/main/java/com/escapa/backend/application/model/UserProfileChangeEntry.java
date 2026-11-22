package com.escapa.backend.application.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileChangeEntry(
        UUID id,
        UUID userId,
        UUID changedBy,
        String fieldName,
        String oldValue,
        String newValue,
        LocalDateTime createdAt
) {
}
