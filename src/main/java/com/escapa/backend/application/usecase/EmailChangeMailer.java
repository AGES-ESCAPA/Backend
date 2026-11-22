package com.escapa.backend.application.usecase;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.escapa.backend.application.port.EmailSenderPort;
import com.escapa.backend.application.port.EmailTemplatePort;

/**
 * Envia o e-mail de confirmação de troca de endereço. Roda fora da transação do pedido.
 */
public class EmailChangeMailer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailChangeMailer.class);
    private static final String SUBJECT = "Confirme seu novo e-mail — Escapa!";
    private static final String TEMPLATE = "confirmacao-email";

    private final EmailSenderPort emailSenderPort;
    private final EmailTemplatePort emailTemplatePort;
    private final String frontendUrl;

    public EmailChangeMailer(
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
    public boolean send(String name, String newEmail, String token) {
        final Map<String, String> variables = Map.of(
                "nome", name,
                "novoEmail", newEmail,
                "link", frontendUrl + "/confirmar-email?token=" + token);
        try {
            emailSenderPort.send(
                    newEmail,
                    SUBJECT,
                    emailTemplatePort.render(TEMPLATE + ".html", variables),
                    emailTemplatePort.render(TEMPLATE + ".txt", variables));
            return true;
        } catch (RuntimeException e) {
            LOGGER.warn("Could not send the email-change confirmation e-mail", e);
            return false;
        }
    }
}
