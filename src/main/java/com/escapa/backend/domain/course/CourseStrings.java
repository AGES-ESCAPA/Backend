package com.escapa.backend.domain.course;

/**
 * Normalização compartilhada de campos textuais de curso.
 */
public final class CourseStrings {

    private CourseStrings() {
    }

    /** {@code null} e branco viram {@code null}; demais valores são trimados. */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
