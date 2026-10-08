package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.dto.PageResult;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

/**
 * Envelope genérico para respostas paginadas.
 * Formato: {@code {content, pageNumber, pageSize, totalElements, totalPages}}.
 */
@Schema(description = "Page of results with its pagination metadata")
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    /** Converte uma página da camada de aplicação, mapeando cada item para o DTO de saída. */
    public static <S, T> PageResponse<T> of(PageResult<S> result, Function<S, T> mapper) {
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements(), result.totalPages());
    }
}
