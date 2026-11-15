package com.escapa.backend.application.model;

import com.escapa.backend.domain.entity.User;

/**
 * Colaborador recém-cadastrado, com o convite que ainda precisa ser enviado por e-mail.
 *
 * @param inviteToken token em claro: existe só aqui e no link do e-mail, o banco guarda apenas o hash
 */
public record RegisteredEmployee(User employee, int assignedCourses, String companyName, String inviteToken) {
}
