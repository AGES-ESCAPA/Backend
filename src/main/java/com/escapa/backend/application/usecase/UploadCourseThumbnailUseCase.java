package com.escapa.backend.application.usecase;

import java.io.InputStream;
import java.util.UUID;

import com.escapa.backend.application.port.FileStoragePort;
import com.escapa.backend.domain.course.InvalidCourseThumbnailException;
import com.escapa.backend.domain.shared.AllowedImageTypes;

/**
 * Envia a imagem de capa de um curso ao storage e devolve a URL publica.
 * Quem chama grava essa URL no campo {@code thumbnailUrl} ao salvar o curso.
 */
public class UploadCourseThumbnailUseCase {

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final FileStoragePort fileStoragePort;

    public UploadCourseThumbnailUseCase(FileStoragePort fileStoragePort) {
        this.fileStoragePort = fileStoragePort;
    }

    public String execute(InputStream content, long size, String contentType) {
        if (!AllowedImageTypes.isAllowed(contentType)) {
            throw InvalidCourseThumbnailException.unsupportedType(contentType);
        }
        if (size <= 0) {
            throw InvalidCourseThumbnailException.empty();
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw InvalidCourseThumbnailException.tooLarge(MAX_FILE_SIZE_BYTES);
        }

        final String key = "courses/thumbnails/" + UUID.randomUUID() + "."
                + AllowedImageTypes.extensionOf(contentType);
        return fileStoragePort.upload(key, content, size, contentType);
    }
}
