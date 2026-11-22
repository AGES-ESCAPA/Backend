package com.escapa.backend.domain.user;

public class EmailChangeTokenExpiredException extends RuntimeException {

    public EmailChangeTokenExpiredException() {
        super("Email change token expired");
    }
}
