package com.escapa.backend.domain.user;

/** Token inexistente ou já usado: a mesma resposta nos dois casos, para não revelar que o token existiu. */
public class InviteNotFoundException extends RuntimeException {

    public InviteNotFoundException() {
        super("Invite not found");
    }
}
