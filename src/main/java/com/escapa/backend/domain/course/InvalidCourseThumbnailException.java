package com.escapa.backend.domain.course;

public class InvalidCourseThumbnailException extends RuntimeException {

    private InvalidCourseThumbnailException(String message) {
        super(message);
    }

    public static InvalidCourseThumbnailException unsupportedType(String contentType) {
        return new InvalidCourseThumbnailException("Unsupported file type: " + contentType);
    }

    public static InvalidCourseThumbnailException tooLarge(long maxBytes) {
        return new InvalidCourseThumbnailException("File exceeds the maximum size of " + maxBytes + " bytes");
    }
}
