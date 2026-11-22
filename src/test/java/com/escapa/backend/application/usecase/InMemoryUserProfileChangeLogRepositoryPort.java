package com.escapa.backend.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.port.UserProfileChangeLogRepositoryPort;

public final class InMemoryUserProfileChangeLogRepositoryPort implements UserProfileChangeLogRepositoryPort {

    public record Entry(UUID userId, UUID changedBy, String fieldName, String oldValue, String newValue) {
    }

    private final List<Entry> entries = new ArrayList<>();

    @Override
    public void append(UUID userId, UUID changedBy, String fieldName, String oldValue, String newValue) {
        entries.add(new Entry(userId, changedBy, fieldName, oldValue, newValue));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }
}
