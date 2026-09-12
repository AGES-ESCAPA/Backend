package com.escapa.backend.adapters.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * @param contentIds todos os conteudos do modulo, na ordem final desejada
 */
public record ReorderContentsRequest(
        @NotEmpty(message = "contentIds is required") List<UUID> contentIds
) {
}
