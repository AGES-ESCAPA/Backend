package com.escapa.backend.domain.content;

import java.util.List;
import java.util.UUID;

public record QuizQuestion(UUID id, String prompt, List<QuizOption> options) {
}