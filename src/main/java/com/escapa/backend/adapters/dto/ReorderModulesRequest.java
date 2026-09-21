package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * @param moduleIds todos os modulos do curso, na ordem final desejada
 */
public record ReorderModulesRequest(
        @NotEmpty(message = "moduleIds is required") List<UUID> moduleIds
) {
}
