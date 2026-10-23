package com.escapa.backend.domain.user;

import java.util.UUID;

public class UserAlreadyInactiveException extends RuntimeException {

    public UserAlreadyInactiveException(UUID userId) {
        super("User is already inactive: " + userId);
    }
}