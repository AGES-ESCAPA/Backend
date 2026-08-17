package com.escapa.backend.domain.user;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String id) {
        super("User not found: " + id);
    }
}
