package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.port.EmailTemplatePort;

class PasswordResetMailerTest {

    private final FakeEmailSender sender = new FakeEmailSender();
    private final EmailTemplatePort templates =
            (name, variables) -> name + "|" + variables.get("nome") + "|" + variables.get("link");
    private final PasswordResetMailer mailer =
            new PasswordResetMailer(sender, templates, "https://app.escapa.com.br");

    @Test
    void shouldSendTheResetEmailWithTheRedefinePasswordLink() {
        final boolean sent = mailer.send("Ana", "ana@escapa.com", "abc123");

        assertTrue(sent);
        final FakeEmailSender.Sent email = sender.sent().get(0);
        assertEquals("ana@escapa.com", email.to());
        final String link = "https://app.escapa.com.br/redefinir-senha?token=abc123";
        assertEquals("recuperacao-senha.html|Ana|" + link, email.htmlBody());
        assertEquals("recuperacao-senha.txt|Ana|" + link, email.textBody());
    }

    @Test
    void shouldNotDoubleTheSlashWhenTheFrontendUrlEndsWithOne() {
        new PasswordResetMailer(sender, templates, "https://app.escapa.com.br/")
                .send("Ana", "ana@escapa.com", "abc123");

        assertTrue(sender.sent().get(0).htmlBody()
                .endsWith("https://app.escapa.com.br/redefinir-senha?token=abc123"));
    }

    @Test
    void shouldReportFailureInsteadOfThrowingWhenTheEmailCannotBeSent() {
        sender.failWith(new IllegalStateException("smtp down"));

        final boolean sent = mailer.send("Ana", "ana@escapa.com", "abc123");

        assertFalse(sent);
    }
}
