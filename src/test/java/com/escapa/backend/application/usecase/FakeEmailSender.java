package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.EmailSenderPort;

import java.util.ArrayList;
import java.util.List;

/** Guarda os e-mails enviados; {@link #failWith} faz o próximo envio falhar. */
public final class FakeEmailSender implements EmailSenderPort {

    public record Sent(String to, String subject, String htmlBody, String textBody) {
    }

    private final List<Sent> sent = new ArrayList<>();
    private RuntimeException failure;

    void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    List<Sent> sent() {
        return sent;
    }

    @Override
    public void send(String to, String subject, String htmlBody, String textBody) {
        if (failure != null) {
            throw failure;
        }
        sent.add(new Sent(to, subject, htmlBody, textBody));
    }
}
