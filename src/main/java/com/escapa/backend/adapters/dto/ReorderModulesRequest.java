package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * @param moduleIds todos os modulos do curso, na ordem final desejada
 */
public record ReorderModulesRequest(
        @NotEmpty(message = "moduleIds is required") List<@NotNull(message = "moduleIds must not contain null") UUID> moduleIds
) {
}
