package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotBlank;

/** Edicao de titulo; a posicao so muda pelo endpoint de reorder. */
public record UpdateModuleRequest(
        @NotBlank(message = "title is required") String title
) {
}
