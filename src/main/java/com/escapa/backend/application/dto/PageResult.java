package com.escapa.backend.application.dto;

import java.util.List;

/**
 * Record genérico para resultados paginados na camada de aplicação.
 * Independente de frameworks — não depende de Spring {@code Page<T>}.
 */
public record PageResult<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
