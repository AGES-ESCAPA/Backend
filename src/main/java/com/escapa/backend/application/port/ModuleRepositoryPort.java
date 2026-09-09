package com.escapa.backend.application.port;

import java.util.UUID;

public interface ModuleRepositoryPort {
    boolean existsById(UUID id);
}
