package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;
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
        static Entry from(ChangeLogEntry entry) {
            return new Entry(
                    entry.id(), entry.description(), entry.changedByName(),
                    entry.majorVersion() + "." + entry.minorVersion(),
                    entry.createdAt() == null ? null : entry.createdAt().toString());
        }
    }


    public static ChangeLogPageResponse from(PageResult<ChangeLogEntry> result) {
        return new ChangeLogPageResponse(
                result.content().stream().map(Entry::from).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements(), result.totalPages());
    }
}
