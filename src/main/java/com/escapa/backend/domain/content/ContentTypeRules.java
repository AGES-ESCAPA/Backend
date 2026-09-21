package com.escapa.backend.domain.content;

import com.escapa.backend.domain.entity.Content;

/**
 * Campos obrigatorios variam conforme o tipo da aula: VIDEO precisa de player e
 * duracao, FILE precisa do arquivo e TEXT precisa do proprio texto.
 */
public final class ContentTypeRules {

    private ContentTypeRules() {
    }

    public static void validate(Content content) {
        if (content.getType() == null) {
            throw new IllegalArgumentException("Type is required");
        }
        switch (content.getType()) {
            case VIDEO -> validateVideo(content);
            case FILE -> requireUrl(content);
            case TEXT -> requireDescription(content);
            default -> throw new IllegalArgumentException("Unsupported content type: " + content.getType());
        }
    }

    private static void validateVideo(Content content) {
        requireUrl(content);
        if (content.getDurationMinutes() == null) {
            throw new IllegalArgumentException("durationMinutes is required for content type VIDEO");
        }
        if (content.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("durationMinutes must be greater than zero");
        }
    }

    private static void requireUrl(Content content) {
        if (isBlank(content.getUrl())) {
            throw new IllegalArgumentException("url is required for content type " + content.getType());
        }
    }

    private static void requireDescription(Content content) {
        if (isBlank(content.getDescription())) {
            throw new IllegalArgumentException("description is required for content type TEXT");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }
}
