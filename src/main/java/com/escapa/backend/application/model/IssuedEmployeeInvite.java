package com.escapa.backend.application.model;

import com.escapa.backend.domain.entity.User;

/** Novo convite emitido para um colaborador pendente, ainda por enviar por e-mail. */
public record IssuedEmployeeInvite(User employee, String companyName, String inviteToken) {
}
