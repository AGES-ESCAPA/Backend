package com.escapa.backend.domain.content;

import java.util.List;
import java.util.UUID;

public record StudentQuizQuestion(UUID id, String prompt, List<StudentQuizOption> options) {
}