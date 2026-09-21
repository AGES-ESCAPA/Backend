package com.escapa.backend.domain.module;

import java.util.UUID;

public class ModuleNotFoundException extends RuntimeException {

    public ModuleNotFoundException(UUID id) {
        super("Module not found: " + id);
    }
}
