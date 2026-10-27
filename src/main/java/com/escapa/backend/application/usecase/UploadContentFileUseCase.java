package com.escapa.backend.application.usecase;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.escapa.backend.application.port.FileStoragePort;
import com.escapa.backend.domain.content.InvalidContentFileException;

/**
 * Envia o arquivo de uma aula do tipo FILE (ou material complementar) ao storage e
 * devolve a URL publica, que o cliente grava depois no campo {@code url} da aula.
 */
public class UploadContentFileUseCase {

    public static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;

    // A extensao decide o tipo: navegadores no Windows mandam CSV como application/vnd.ms-excel.
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls", "application/vnd.ms-excel",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "csv", "text/csv"
    );

    private final FileStoragePort fileStoragePort;

    public UploadContentFileUseCase(FileStoragePort fileStoragePort) {
        this.fileStoragePort = fileStoragePort;
    }

    public String execute(String originalFileName, InputStream content, long size) {
        final String extension = extensionOf(originalFileName);
        if (!ALLOWED_EXTENSIONS.containsKey(extension)) {
            throw InvalidContentFileException.unsupportedType(originalFileName);
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw InvalidContentFileException.tooLarge(MAX_FILE_SIZE_BYTES);
        }

        final String key = "contents/" + UUID.randomUUID() + "." + extension;
        return fileStoragePort.upload(key, content, size, ALLOWED_EXTENSIONS.get(extension));
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        final int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
