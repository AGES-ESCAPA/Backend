package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class InMemoryEnrollmentRepositoryPort implements EnrollmentRepositoryPort {
    private record Key(UUID userId, UUID courseId) {
    }

    private final Map<Key, Enrollment> enrollments = new HashMap<>();

    void enroll(UUID userId, UUID courseId, Enrollment enrollment) {
        enrollments.put(new Key(userId, courseId), enrollment);
    }

    @Override
    public Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId) {
        return Optional.ofNullable(enrollments.get(new Key(userId, courseId)));
    }
}
