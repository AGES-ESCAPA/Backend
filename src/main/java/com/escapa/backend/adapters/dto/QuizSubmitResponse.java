package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.QuizSubmitResult;

public record QuizSubmitResponse(
        int scorePercent,
        boolean passed,
        int correctCount,
        int totalQuestions
) {
    public static QuizSubmitResponse from(QuizSubmitResult result) {
        return new QuizSubmitResponse(
                result.scorePercent(),
                result.passed(),
                result.correctCount(),
                result.totalQuestions());
    }
}