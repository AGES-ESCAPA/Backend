package com.escapa.backend.adapters.dto;

import java.util.List;
import java.util.UUID;

public record ChangeLogPageResponse(
        List<Entry> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record Entry(
            UUID id,
            String description,
            String changedBy,
            String version,
            String createdAt
    ) {
    }
}
