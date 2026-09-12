package com.escapa.backend.adapters.dto;

import java.util.List;

/**
 * Envelope genérico para respostas paginadas.
 * Formato: {@code {content, pageNumber, pageSize, totalElements, totalPages}}.
 */
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
