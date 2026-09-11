package com.escapa.backend.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.entity.Content;

final class InMemoryContentRepositoryPort implements ContentRepositoryPort {
    private final List<Content> contents = new ArrayList<>();

    @Override
    public Content save(Content content) {
        // Espelha o adapter real: id nulo vira um UUID gerado na persistencia, e
        // id preenchido que nao existe e erro, nunca uma criacao silenciosa.
        if (content.getId() == null) {
            content.setId(UUID.randomUUID());
        } else if (findById(content.getId()).isEmpty()) {
            throw new ContentNotFoundException(content.getId());
        }
        contents.removeIf(c -> content.getId().equals(c.getId()));
        contents.add(content);
        return content;
    }

    @Override
    public Optional<Content> findById(UUID id) {
        return contents.stream().filter(content -> content.getId().equals(id)).findFirst();
    }

    @Override
    public List<Content> findByModuleId(UUID moduleId) {
        return contents.stream()
                .filter(content -> moduleId.equals(content.getModuleId()))
                .sorted(Comparator.comparing(Content::getOrder))
                .toList();
    }

    @Override
    public int nextOrder(UUID moduleId) {
        return findByModuleId(moduleId).stream()
                .mapToInt(Content::getOrder)
                .max()
                .orElse(0) + 1;
    }

    @Override
    public void deleteById(UUID id) {
        contents.removeIf(content -> content.getId().equals(id));
    }

    @Override
    public void reorder(UUID moduleId, List<UUID> orderedIds) {
        for (int index = 0; index < orderedIds.size(); index++) {
            final int position = index + 1;
            findById(orderedIds.get(index)).ifPresent(content -> content.setOrder(position));
        }
    }
}
