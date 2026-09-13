package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.module.ModuleNotFoundException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryModuleRepositoryPort implements ModuleRepositoryPort {
    private final List<Module> modules = new ArrayList<>();

    /** Modulo avulso, em um curso qualquer; mantido para os testes de conteudo (BE-05). */
    UUID createModule() {
        return createModule(UUID.randomUUID());
    }

    UUID createModule(UUID courseId) {
        final Module module = new Module(UUID.randomUUID(), courseId, "Modulo", nextOrder(courseId));
        modules.add(module);
        return module.getId();
    }

    @Override
    public boolean existsById(UUID id) {
        return findById(id).isPresent();
    }

    @Override
    public Module save(Module module) {
        // Espelha o adapter real: id nulo vira um UUID gerado na persistencia, e
        // id preenchido que nao existe e erro, nunca uma criacao silenciosa.
        if (module.getId() == null) {
            module.setId(UUID.randomUUID());
        } else if (findById(module.getId()).isEmpty()) {
            throw new ModuleNotFoundException(module.getId());
        }
        modules.removeIf(m -> module.getId().equals(m.getId()));
        modules.add(module);
        return module;
    }

    @Override
    public Optional<Module> findById(UUID id) {
        return modules.stream().filter(module -> module.getId().equals(id)).findFirst();
    }

    @Override
    public List<Module> findByCourseId(UUID courseId) {
        return modules.stream()
                .filter(module -> courseId.equals(module.getCourseId()))
                .sorted(Comparator.comparing(Module::getOrder))
                .toList();
    }

    @Override
    public int nextOrder(UUID courseId) {
        return findByCourseId(courseId).stream()
                .mapToInt(Module::getOrder)
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
            findById(orderedIds.get(index)).ifPresent(module -> module.setOrder(position));
        }
    }
}
