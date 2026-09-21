package com.escapa.backend.domain.course;

import java.util.List;

public class CourseValidationException extends RuntimeException {
    private final List<String> missingFields;

    public CourseValidationException(String message, List<String> missingFields) {
        super(message);
        this.missingFields = missingFields;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}

