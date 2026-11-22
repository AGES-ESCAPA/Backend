package com.escapa.backend.domain.user;

public class EmailChangeTokenNotFoundException extends RuntimeException {

    public EmailChangeTokenNotFoundException() {
        super("Email change token not found");
    }
}
