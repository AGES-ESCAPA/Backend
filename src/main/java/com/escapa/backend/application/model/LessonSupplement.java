package com.escapa.backend.application.model;

import java.util.List;

public record LessonSupplement(List<LessonConcept> concepts, List<LessonReference> references) {
}
