package com.escapa.backend.application.port;

import com.escapa.backend.application.model.LessonSupplement;

import java.util.UUID;

public interface LessonSupplementRepositoryPort {
    LessonSupplement getSupplementsByLessonId(UUID lessonId);
}
