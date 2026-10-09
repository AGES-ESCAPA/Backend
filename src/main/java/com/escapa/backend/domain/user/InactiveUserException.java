package com.escapa.backend.domain.user;

public class InactiveUserException extends RuntimeException {

    public InactiveUserException(String message) {
        super(message);
    }

    public InactiveUserException() {
        super("User account is inactive");
    }
}

