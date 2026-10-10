package com.escapa.backend.domain.user;

public class InvalidAvatarFileException extends RuntimeException {

    private InvalidAvatarFileException(String message) {
        super(message);
    }

    public static InvalidAvatarFileException unsupportedType(String contentType) {
        return new InvalidAvatarFileException("Unsupported file type: " + contentType);
    }

    public static InvalidAvatarFileException tooLarge(long maxBytes) {
        return new InvalidAvatarFileException("File exceeds the maximum size of " + maxBytes + " bytes");
    }
}