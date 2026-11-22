package com.escapa.backend.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.entity.Content;

/**
 * Fake em memória. Grava e devolve cópias: alterar a aula lida não muda o que
 * está salvo até {@link #save}.
 */
public final class InMemoryContentRepositoryPort implements ContentRepositoryPort {
    private final List<Content> contents = new ArrayList<>();
    private final List<Content> savedSnapshots = new ArrayList<>();
    private int saveCount;

    @Override
    public Content save(Content content) {
        // Espelha o adapter real: id nulo vira um UUID gerado na persistencia, e
        // id preenchido que nao existe e erro, nunca uma criacao silenciosa.
        if (content.getId() == null) {
            content.setId(UUID.randomUUID());
        } else if (storedById(content.getId()).isEmpty()) {
            throw new ContentNotFoundException(content.getId());
        }
        final Content stored = content.copy();
        contents.removeIf(current -> stored.getId().equals(current.getId()));
        contents.add(stored);
        savedSnapshots.add(stored.copy());
        saveCount++;
        return stored.copy();
    }

    public int saveCount() {
        return saveCount;
    }

    public List<Content> savedSnapshots() {
        return savedSnapshots.stream().map(Content::copy).toList();
    }

    @Override
    public Optional<Content> findById(UUID id) {
        return storedById(id).map(Content::copy);
    }

    @Override
    public List<Content> findByModuleId(UUID moduleId) {
        return contents.stream()
                .filter(content -> moduleId.equals(content.getModuleId()))
                .sorted(Comparator.comparing(Content::getOrder))
                .map(Content::copy)
                .toList();
    }

    @Override
    public int nextOrder(UUID moduleId) {
        return findByModuleId(moduleId).stream()
                .map(Content::getOrder)
                .filter(order -> order != null && order > 0)
                .mapToInt(Integer::intValue)
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
            storedById(orderedIds.get(index)).ifPresent(content -> content.setOrder(position));
        }
    }

    private Optional<Content> storedById(UUID id) {
        return contents.stream().filter(content -> id.equals(content.getId())).findFirst();
    }
}
