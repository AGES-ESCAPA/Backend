package com.escapa.backend.application.usecase;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.escapa.backend.application.port.EmailSenderPort;
import com.escapa.backend.application.port.EmailTemplatePort;

/**
 * Envia o e-mail de recuperacao de senha. Roda fora da transacao do pedido: falha
 * no envio nao desfaz o token, so e informada.
 */
public class PasswordResetMailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetMailer.class);
    private static final String SUBJECT = "Redefinir senha — Escapa!";
    private static final String TEMPLATE = "recuperacao-senha";

    private final EmailSenderPort emailSenderPort;
    private final EmailTemplatePort emailTemplatePort;
    private final String frontendUrl;

    public PasswordResetMailer(
            EmailSenderPort emailSenderPort,
            EmailTemplatePort emailTemplatePort,
            String frontendUrl
    ) {
        this.emailSenderPort = emailSenderPort;
        this.emailTemplatePort = emailTemplatePort;
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    /** @return {@code true} se o e-mail foi enviado; {@code false} se o envio falhou */
    public boolean send(String name, String email, String token) {
        final Map<String, String> variables = Map.of(
                "titulo", "Redefinir senha — Escapa!",
                "preheader", "Recebemos um pedido para redefinir a senha da sua conta.",
                "nome", name,
                "link", frontendUrl + "/redefinir-senha?token=" + token);
        try {
            emailSenderPort.send(
                    email,
                    SUBJECT,
                    emailTemplatePort.render(TEMPLATE + ".html", variables),
                    emailTemplatePort.render(TEMPLATE + ".txt", variables));
            return true;
        } catch (RuntimeException e) {
            LOGGER.warn("Could not send the password reset e-mail", e);
            return false;
        }
    }
}
