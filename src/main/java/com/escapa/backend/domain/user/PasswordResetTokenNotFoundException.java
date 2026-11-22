package com.escapa.backend.domain.user;

/** Token de recuperacao inexistente ou ja usado. */
public class PasswordResetTokenNotFoundException extends RuntimeException {

    public PasswordResetTokenNotFoundException() {
        super("Password reset token not found");
    }
}
