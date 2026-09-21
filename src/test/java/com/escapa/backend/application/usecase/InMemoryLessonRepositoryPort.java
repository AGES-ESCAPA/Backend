package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.port.LessonRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryLessonRepositoryPort implements LessonRepositoryPort {
    private final List<LessonDetails> lessons = new ArrayList<>();

    void add(LessonDetails lesson) {
        lessons.add(lesson);
    }

    @Override
    public Optional<LessonDetails> findByCourseIdAndId(UUID courseId, UUID lessonId) {
        return lessons.stream()
                .filter(lesson -> lesson.courseId().equals(courseId))
                .filter(lesson -> lesson.content().getId().equals(lessonId))
                .findFirst();
    }
}
