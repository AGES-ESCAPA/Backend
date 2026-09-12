package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class InMemoryCourseRepositoryPort implements CourseRepositoryPort {
    private final Map<UUID, CourseDetails> courses = new HashMap<>();

    void save(CourseDetails course) {
        courses.put(course.id(), course);
    }

    @Override
    public Optional<CourseDetails> findDetailsById(UUID id) {
        return Optional.ofNullable(courses.get(id));
    }
}
