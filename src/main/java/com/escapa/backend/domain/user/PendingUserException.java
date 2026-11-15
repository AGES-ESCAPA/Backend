package com.escapa.backend.domain.user;

/** Colaborador convidado que ainda não criou a senha pelo link do e-mail (US-31). */
public class PendingUserException extends RuntimeException {

    public PendingUserException() {
        super("Account pending activation. Use the link sent to your e-mail to create your password");
    }
}
