package com.escapa.backend.application.usecase;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.escapa.backend.application.port.EmailSenderPort;
import com.escapa.backend.application.port.EmailTemplatePort;

/**
 * Envia o e-mail do convite do colaborador. Não é um {@code *UseCase}, de propósito: roda fora da
 * transação do cadastro, depois do commit, e uma falha no envio não desfaz nada, só é informada.
 */
public class EmployeeInviteMailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeInviteMailer.class);
    private static final String SUBJECT = "Você foi convidado(a) para a Escapa!";
    private static final String TEMPLATE = "convite-colaborador";

    private final EmailSenderPort emailSenderPort;
    private final EmailTemplatePort emailTemplatePort;
    private final String frontendUrl;

    public EmployeeInviteMailer(EmailSenderPort emailSenderPort, EmailTemplatePort emailTemplatePort,
                                String frontendUrl) {
        this.emailSenderPort = emailSenderPort;
        this.emailTemplatePort = emailTemplatePort;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    /** @return {@code true} se o e-mail foi enviado; {@code false} se o envio falhou */
    public boolean send(String name, String email, String companyName, String token) {
        final Map<String, String> variables = Map.of(
                "nome", name,
                "empresa", companyName,
                "link", frontendUrl + "/criar-senha?token=" + token);
        try {
            emailSenderPort.send(email, SUBJECT,
                    emailTemplatePort.render(TEMPLATE + ".html", variables),
                    emailTemplatePort.render(TEMPLATE + ".txt", variables));
            return true;
        } catch (RuntimeException e) {
            // O token não entra no log: ele é a credencial do link.
            LOGGER.warn("Could not send the invite e-mail to the employee; it can be resent", e);
            return false;
        }
    }
}
