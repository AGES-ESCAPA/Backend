package com.escapa.backend.application.port;

import java.util.UUID;

import com.escapa.backend.application.model.LessonSupplement;

public interface LessonSupplementRepositoryPort {
    LessonSupplement getSupplementsByLessonId(UUID lessonId);
}
