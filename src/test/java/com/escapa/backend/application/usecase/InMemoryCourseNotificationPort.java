package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseNotificationPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fake em memória de {@link CourseNotificationPort} para testes unitários.
 */
final class InMemoryCourseNotificationPort implements CourseNotificationPort {

    private final List<UUID> notifiedCourseIds = new ArrayList<>();

    List<UUID> notifiedCourseIds() {
        return notifiedCourseIds;
    }

    @Override
    public void notifyCoursePublished(UUID courseId) {
        notifiedCourseIds.add(courseId);
    }
}
