package com.escapa.backend.domain.company;

public class DuplicateRegistrationException extends RuntimeException {

    private final String field;

    public DuplicateRegistrationException(String field) {
        super(field + " already registered");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}