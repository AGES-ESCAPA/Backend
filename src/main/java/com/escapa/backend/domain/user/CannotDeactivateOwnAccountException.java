package com.escapa.backend.domain.user;

/** Impede que o admin bloqueie a própria conta pela gestão de usuários. */
public class CannotDeactivateOwnAccountException extends RuntimeException {

    public CannotDeactivateOwnAccountException() {
        super("Administrators cannot deactivate their own account");
    }
}
