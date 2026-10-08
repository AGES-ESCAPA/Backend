package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de criação e edição de módulo (apenas o título). {@code order} não é aceito: o módulo
 * novo sempre entra no fim da lista do curso e a posição só muda pelo endpoint de reorder.
 */
public record ModuleRequest(
        @NotBlank(message = "title is required")
        @Size(max = FieldLimits.VARCHAR_MAX, message = "title " + FieldLimits.VARCHAR_MESSAGE)
        String title
) {
}
