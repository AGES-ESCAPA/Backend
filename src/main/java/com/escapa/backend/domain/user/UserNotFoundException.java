package com.escapa.backend.domain.user;

import com.escapa.backend.common.exception.NotFoundException;

import java.util.UUID;

public class UserNotFoundException extends NotFoundException {

    public static final String CODE = "USER_NOT_FOUND";

    public UserNotFoundException(UUID id) {
        super(CODE, "User not found: " + id);
    }
}
