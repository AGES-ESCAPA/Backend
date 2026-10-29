package com.escapa.backend.domain.content;

/**
 * Material complementar da aula: um link externo ({@code link}) ou um arquivo ja
 * enviado ao storage ({@code file}). Persistido como JSON na coluna {@code recursos}.
 */
public record ContentResource(String id, String type, String title, String url) {

    public static final String TYPE_LINK = "link";
    public static final String TYPE_FILE = "file";
}
