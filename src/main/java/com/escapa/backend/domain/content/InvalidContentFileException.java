package com.escapa.backend.domain.content;

public class InvalidContentFileException extends RuntimeException {

    private InvalidContentFileException(String message) {
        super(message);
    }

    public static InvalidContentFileException unsupportedType(String fileName) {
        return new InvalidContentFileException("Unsupported file type: " + fileName);
    }

    public static InvalidContentFileException tooLarge(long maxBytes) {
        return new InvalidContentFileException("File exceeds the maximum size of " + maxBytes + " bytes");
    }
}
