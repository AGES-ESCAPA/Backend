package com.escapa.backend.domain.user;

public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException(String field) {
        super(field + " already registered");
    }
}
