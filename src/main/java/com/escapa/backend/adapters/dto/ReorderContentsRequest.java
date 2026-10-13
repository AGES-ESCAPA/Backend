package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * @param contentIds todos os conteudos do modulo, na ordem final desejada
 */
@Schema(description = "All lesson ids of a module in the desired final order")
public record ReorderContentsRequest(
        @NotEmpty(message = "contentIds is required") List<@NotNull(message = "contentIds must not contain null") UUID> contentIds
) {
}
