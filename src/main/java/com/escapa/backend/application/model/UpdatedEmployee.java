package com.escapa.backend.application.model;

/**
 * Colaborador depois da edição pelo gestor (US-32).
 *
 * @param inviteToken token de um convite novo, ainda por enviar por e-mail; nulo quando a edição não
 *                    gerou convite
 * @param companyName nome da empresa, para o e-mail do convite; nulo quando não há convite
 */
public record UpdatedEmployee(CompanyEmployee employee, String companyName, String inviteToken) {

    public boolean hasNewInvite() {
        return inviteToken != null;
    }
}
