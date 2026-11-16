package com.escapa.backend.domain.user;

public class InviteExpiredException extends RuntimeException {

    public InviteExpiredException() {
        super("Invite has expired");
    }
}
