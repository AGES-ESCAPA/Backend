package com.escapa.backend.application.model;

public record QuizSubmitResult(int scorePercent, boolean passed, int correctCount, int totalQuestions) {
}