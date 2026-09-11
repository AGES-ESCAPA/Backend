package com.escapa.backend.user.exception;

import com.escapa.backend.common.exception.ConflictException;

public class EmailAlreadyUsedException extends ConflictException {

    public static final String CODE = "EMAIL_ALREADY_USED";

    public EmailAlreadyUsedException(String email) {
        super(CODE, "Email already in use: " + email);
    }
}
