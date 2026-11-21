package com.escapa.backend.domain.user;

/** Token de recuperacao expirado. */
public class PasswordResetTokenExpiredException extends RuntimeException {

    public PasswordResetTokenExpiredException() {
        super("Password reset token has expired");
    }
}
