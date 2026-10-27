package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.LessonNotificationPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fake em memória de {@link LessonNotificationPort} para testes unitários.
 */
public final class InMemoryLessonNotificationPort implements LessonNotificationPort {

    private final List<UUID> notifiedLessonIds = new ArrayList<>();
    private final List<String> notifiedLessonTitles = new ArrayList<>();

    public List<UUID> notifiedLessonIds() {
        return notifiedLessonIds;
    }

    public List<String> notifiedLessonTitles() {
        return notifiedLessonTitles;
    }

    @Override
    public void notifyLessonUpdated(UUID lessonId, String lessonTitle) {
        notifiedLessonIds.add(lessonId);
        notifiedLessonTitles.add(lessonTitle);
    }
}
