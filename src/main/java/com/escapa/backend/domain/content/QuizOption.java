package com.escapa.backend.domain.content;

import java.util.UUID;

public record QuizOption(UUID id, String text, boolean correct) {
}