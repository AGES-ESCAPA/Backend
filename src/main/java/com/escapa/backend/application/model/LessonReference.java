package com.escapa.backend.application.model;

import java.util.UUID;

public record LessonReference(UUID id, String title, String url, Integer order) {
}
