package com.escapa.backend.application.usecase;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import com.escapa.backend.application.port.FileStoragePort;
import com.escapa.backend.domain.course.InvalidCourseThumbnailException;

/**
 * Envia a imagem de capa de um curso ao storage e devolve a URL publica.
 * Quem chama grava essa URL no campo {@code thumbnailUrl} ao salvar o curso.
 */
public class UploadCourseThumbnailUseCase {

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final FileStoragePort fileStoragePort;

    public UploadCourseThumbnailUseCase(FileStoragePort fileStoragePort) {
        this.fileStoragePort = fileStoragePort;
    }

    public String execute(InputStream content, long size, String contentType) {
        if (!ALLOWED_CONTENT_TYPES.containsKey(contentType)) {
            throw InvalidCourseThumbnailException.unsupportedType(contentType);
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw InvalidCourseThumbnailException.tooLarge(MAX_FILE_SIZE_BYTES);
        }

        final String key = "courses/thumbnails/" + UUID.randomUUID() + "." + ALLOWED_CONTENT_TYPES.get(contentType);
        return fileStoragePort.upload(key, content, size, contentType);
    }
}
