package com.escapa.backend.domain.content;

import com.escapa.backend.domain.entity.Content;

/**
 * Campos obrigatorios variam conforme o tipo da aula: VIDEO precisa de player e
 * duracao, FILE precisa do arquivo e TEXT precisa do proprio texto. Materiais
 * complementares valem para qualquer tipo.
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
        if (content.getResources() != null) {
            content.getResources().forEach(ContentTypeRules::validateResource);
        }
    }

    private static void validateResource(ContentResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resources must not contain null items");
        }
        if (!ContentResource.TYPE_LINK.equals(resource.type()) && !ContentResource.TYPE_FILE.equals(resource.type())) {
            throw new IllegalArgumentException("resources.type must be 'link' or 'file'");
        }
        if (isBlank(resource.title())) {
            throw new IllegalArgumentException("resources.title is required");
        }
        if (isBlank(resource.url())) {
            throw new IllegalArgumentException("resources.url is required");
        }
        final String url = resource.url().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("resources.url must start with http:// or https://");
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
