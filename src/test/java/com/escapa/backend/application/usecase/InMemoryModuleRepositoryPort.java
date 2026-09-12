package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ModuleRepositoryPort;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class InMemoryModuleRepositoryPort implements ModuleRepositoryPort {
    private final Set<UUID> modules = new HashSet<>();

    UUID createModule() {
        final UUID id = UUID.randomUUID();
        modules.add(id);
        return id;
    }

    @Override
    public boolean existsById(UUID id) {
        return modules.contains(id);
    }
}
