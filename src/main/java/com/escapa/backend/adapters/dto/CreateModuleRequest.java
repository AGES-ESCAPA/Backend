package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code order} nao e aceito: o modulo novo sempre entra no fim da lista do curso. */
public record CreateModuleRequest(
        @NotBlank(message = "title is required") String title
) {
}
