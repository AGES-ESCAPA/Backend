package com.escapa.backend.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope de resposta paginada.
 * Formato: {@code {content, pageNumber, pageSize, totalElements, totalPages}}.
 *
 * <p>Endpoints paginados devolvem este envelope direto, sem {@link ApiResponse},
 * por contrato com o frontend.
 */
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
