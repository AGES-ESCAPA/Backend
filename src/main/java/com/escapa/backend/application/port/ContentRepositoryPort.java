package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.Content;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepositoryPort {
    Content save(Content content);

    Optional<Content> findById(UUID id);

    List<Content> findByModuleId(UUID moduleId);

    /** Proximo valor livre de ordem no modulo: max(order) + 1, ou 1 quando vazio. */
    int nextOrder(UUID moduleId);

    void deleteById(UUID id);

    /**
     * Regrava a ordem dos conteudos do modulo em duas fases, para nao violar
     * uk_content_module_order enquanto as posicoes se cruzam.
     *
     * @param orderedIds ids na ordem final desejada; posicoes atribuidas a partir de 1
     */
    void reorder(UUID moduleId, List<UUID> orderedIds);
}
