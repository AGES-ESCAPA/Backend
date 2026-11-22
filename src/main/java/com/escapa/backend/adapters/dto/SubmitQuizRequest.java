package com.escapa.backend.adapters.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SubmitQuizRequest(@NotEmpty @Valid List<QuizAnswerRequest> answers) {

    public record QuizAnswerRequest(
            @NotNull UUID questionId,
            @NotNull UUID optionId
    ) {
    }
}