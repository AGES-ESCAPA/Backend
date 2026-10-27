package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.LessonProgressRepositoryPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InMemoryLessonProgressRepositoryPort implements LessonProgressRepositoryPort {
    private record Key(UUID userId, UUID lessonId) {
    }

    private final Map<Key, LocalDateTime> progress = new HashMap<>();

    /** Nao filtra por curso: os testes usam ids de aula unicos. */
    @Override
    public List<UUID> findCompletedLessonIds(UUID userId, UUID courseId) {
        final List<UUID> lessonIds = new ArrayList<>();
        progress.keySet().stream()
                .filter(key -> key.userId().equals(userId))
                .forEach(key -> lessonIds.add(key.lessonId()));
        return lessonIds;
    }

    @Override
    public void markCompleted(UUID userId, UUID lessonId, LocalDateTime completedAt) {
        progress.putIfAbsent(new Key(userId, lessonId), completedAt);
    }

    public LocalDateTime completedAt(UUID userId, UUID lessonId) {
        return progress.get(new Key(userId, lessonId));
    }
}
