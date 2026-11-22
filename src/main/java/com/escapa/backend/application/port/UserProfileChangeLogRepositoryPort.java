package com.escapa.backend.application.port;

import java.util.UUID;

public interface UserProfileChangeLogRepositoryPort {

    void append(UUID userId, UUID changedBy, String fieldName, String oldValue, String newValue);
}
