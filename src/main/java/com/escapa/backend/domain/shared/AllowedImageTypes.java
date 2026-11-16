package com.escapa.backend.domain.shared;

import java.util.Map;

/**
 * Tipos MIME de imagem aceitos em upload de avatar e capa de curso.
 */
public final class AllowedImageTypes {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private AllowedImageTypes() {
    }

    public static boolean isAllowed(String contentType) {
        return contentType != null && EXTENSION_BY_CONTENT_TYPE.containsKey(contentType);
    }

    public static String extensionOf(String contentType) {
        return EXTENSION_BY_CONTENT_TYPE.get(contentType);
    }
}
