package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.EmailTemplatePort;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeInviteMailerTest {

    private final FakeEmailSender sender = new FakeEmailSender();
    private final EmailTemplatePort templates =
            (name, variables) -> name + "|" + variables.get("nome") + "|" + variables.get("empresa")
                    + "|" + variables.get("link");
    private final EmployeeInviteMailer mailer =
            new EmployeeInviteMailer(sender, templates, "https://app.escapa.com.br");

    @Test
    void shouldSendTheInviteWithTheCreatePasswordLink() {
        final boolean sent = mailer.send("João", "joao@empresa.com.br", "Hotel Gramado LTDA", "abc123");

        assertTrue(sent);
        final FakeEmailSender.Sent email = sender.sent().get(0);
        assertEquals("joao@empresa.com.br", email.to());
        final String link = "https://app.escapa.com.br/criar-senha?token=abc123";
        assertEquals("convite-colaborador.html|João|Hotel Gramado LTDA|" + link, email.htmlBody());
        assertEquals("convite-colaborador.txt|João|Hotel Gramado LTDA|" + link, email.textBody());
    }

    @Test
    void shouldNotDoubleTheSlashWhenTheFrontendUrlEndsWithOne() {
        new EmployeeInviteMailer(sender, templates, "https://app.escapa.com.br/")
                .send("João", "joao@empresa.com.br", "Hotel", "abc123");

        assertTrue(sender.sent().get(0).htmlBody().endsWith("https://app.escapa.com.br/criar-senha?token=abc123"));
    }

    @Test
    void shouldReportFailureInsteadOfThrowingWhenTheEmailCannotBeSent() {
        sender.failWith(new IllegalStateException("smtp down"));

        final boolean sent = mailer.send("João", "joao@empresa.com.br", "Hotel", "abc123");

        assertFalse(sent);
    }
}
