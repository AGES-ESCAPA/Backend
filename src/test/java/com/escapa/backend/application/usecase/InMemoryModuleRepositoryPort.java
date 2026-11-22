package com.escapa.backend.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.module.ModuleNotFoundException;

/**
 * Fake em memória. Grava e devolve cópias: alterar o módulo lido não muda o que
 * está salvo até {@link #save}.
 */
public final class InMemoryModuleRepositoryPort implements ModuleRepositoryPort {
    private final List<Module> modules = new ArrayList<>();
    private final List<Module> savedSnapshots = new ArrayList<>();
    private int saveCount;

    /** Modulo avulso, em um curso qualquer; mantido para os testes de conteudo (BE-05). */
    public UUID createModule() {
        return createModule(UUID.randomUUID());
    }

    public UUID createModule(UUID courseId) {
        final Module module = new Module(UUID.randomUUID(), courseId, "Modulo", nextOrder(courseId));
        modules.add(module);
        return module.getId();
    }

    @Override
    public boolean existsById(UUID id) {
        return storedById(id).isPresent();
    }

    @Override
    public Module save(Module module) {
        // Espelha o adapter real: id nulo vira um UUID gerado na persistencia, e
        // id preenchido que nao existe e erro, nunca uma criacao silenciosa.
        if (module.getId() == null) {
            module.setId(UUID.randomUUID());
        } else if (storedById(module.getId()).isEmpty()) {
            throw new ModuleNotFoundException(module.getId());
        }
        final Module stored = copyOf(module);
        modules.removeIf(current -> stored.getId().equals(current.getId()));
        modules.add(stored);
        savedSnapshots.add(copyOf(stored));
        saveCount++;
        return copyOf(stored);
    }

    public int saveCount() {
        return saveCount;
    }

    public List<Module> savedSnapshots() {
        return savedSnapshots.stream().map(InMemoryModuleRepositoryPort::copyOf).toList();
    }

    @Override
    public Optional<Module> findById(UUID id) {
        return storedById(id).map(InMemoryModuleRepositoryPort::copyOf);
    }

    @Override
    public List<Module> findByCourseId(UUID courseId) {
        return modules.stream()
                .filter(module -> courseId.equals(module.getCourseId()))
                .sorted(Comparator.comparing(Module::getOrder))
                .map(InMemoryModuleRepositoryPort::copyOf)
                .toList();
    }

    @Override
    public int nextOrder(UUID courseId) {
        return findByCourseId(courseId).stream()
                .map(Module::getOrder)
                .filter(order -> order != null && order > 0)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    @Override
    public void deleteById(UUID id) {
        modules.removeIf(module -> module.getId().equals(id));
    }

    @Override
    public void reorder(UUID courseId, List<UUID> orderedIds) {
        for (int index = 0; index < orderedIds.size(); index++) {
            final int position = index + 1;
            storedById(orderedIds.get(index)).ifPresent(module -> module.setOrder(position));
        }
    }

    private Optional<Module> storedById(UUID id) {
        return modules.stream().filter(module -> id.equals(module.getId())).findFirst();
    }

    private static Module copyOf(Module source) {
        final Module copy = new Module();
        copy.setId(source.getId());
        copy.setCourseId(source.getCourseId());
        copy.setCourse(source.getCourse());
        copy.setTitle(source.getTitle());
        copy.setOrder(source.getOrder());
        copy.setPublishedOrder(source.getPublishedOrder());
        copy.setPendingTitle(source.getPendingTitle());
        copy.setPendingDeletion(source.isPendingDeletion());
        copy.setCoursePublished(source.isCoursePublished());
        copy.setContents(copyContents(source.getContents()));
        return copy;
    }

    private static List<Content> copyContents(List<Content> contents) {
        final List<Content> copies = new ArrayList<>();
        if (contents == null) {
            return copies;
        }
        for (final Content content : contents) {
            copies.add(content.copy());
        }
        return copies;
    }
}
