package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;

import java.util.List;
import java.util.UUID;

/**
 * Modulo com agregados derivados dos conteudos carregados. Os conteudos vem
 * resumidos; o detalhe completo fica nos endpoints de conteudo (BE-05).
 */
public record ModuleResponse(
        UUID id,
        String title,
        Integer order,
        int totalContents,
        int totalDurationMinutes,
        List<ContentSummary> contents
) {
    public static ModuleResponse from(Module module) {
        final List<ContentSummary> contents = module.getContents() == null
                ? List.of()
                : module.getContents().stream().map(ContentSummary::from).toList();
        return new ModuleResponse(
                module.getId(),
                module.getTitle(),
                module.getOrder(),
                module.getTotalContents(),
                module.getTotalDurationMinutes(),
                contents
        );
    }

    public record ContentSummary(
            UUID id,
            String title,
            ContentType type,
            Integer order
    ) {
        static ContentSummary from(Content content) {
            return new ContentSummary(
                    content.getId(),
                    content.getTitle(),
                    content.getType(),
                    content.getOrder()
            );
        }
    }
}
