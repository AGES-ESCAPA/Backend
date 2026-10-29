package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.EnrollmentProgressRepositoryPort;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InMemoryEnrollmentProgressRepositoryPort implements EnrollmentProgressRepositoryPort {
    private record Key(UUID userId, UUID courseId) {
    }

    private final Map<Key, Integer> progress = new HashMap<>();
    private final Map<Key, LocalDate> conclusionDates = new HashMap<>();

    @Override
    public void updateProgress(UUID userId, UUID courseId, int value, LocalDate conclusionDate) {
        final Key key = new Key(userId, courseId);
        progress.put(key, value);
        if (conclusionDate != null) {
            conclusionDates.putIfAbsent(key, conclusionDate);
        }
    }

    public Integer progress(UUID userId, UUID courseId) {
        return progress.get(new Key(userId, courseId));
    }

    public LocalDate conclusionDate(UUID userId, UUID courseId) {
        return conclusionDates.get(new Key(userId, courseId));
    }
}
