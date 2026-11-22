package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.port.EmailTemplatePort;

class EmailChangeMailerTest {

    private final FakeEmailSender sender = new FakeEmailSender();
    private final EmailTemplatePort templates =
            (name, variables) -> name + "|" + variables.get("nome") + "|" + variables.get("link");
    private final EmailChangeMailer mailer =
            new EmailChangeMailer(sender, templates, "https://app.escapa.com.br");

    @Test
    void shouldSendTheConfirmationEmailWithTheConfirmLink() {
        final boolean sent = mailer.send("Ana", "ana.nova@escapa.com", "abc123");

        assertTrue(sent);
        final FakeEmailSender.Sent email = sender.sent().get(0);
        assertEquals("ana.nova@escapa.com", email.to());
        final String link = "https://app.escapa.com.br/confirmar-email?token=abc123";
        assertEquals("confirmacao-email.html|Ana|" + link, email.htmlBody());
        assertEquals("confirmacao-email.txt|Ana|" + link, email.textBody());
    }

    @Test
    void shouldNotDoubleTheSlashWhenTheFrontendUrlEndsWithOne() {
        new EmailChangeMailer(sender, templates, "https://app.escapa.com.br/")
                .send("Ana", "ana.nova@escapa.com", "abc123");

        assertTrue(sender.sent().get(0).htmlBody()
                .endsWith("https://app.escapa.com.br/confirmar-email?token=abc123"));
    }

    @Test
    void shouldReportFailureInsteadOfThrowingWhenTheEmailCannotBeSent() {
        sender.failWith(new IllegalStateException("smtp down"));

        final boolean sent = mailer.send("Ana", "ana.nova@escapa.com", "abc123");

        assertFalse(sent);
    }
}
