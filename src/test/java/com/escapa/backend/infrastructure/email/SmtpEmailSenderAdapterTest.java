package com.escapa.backend.infrastructure.email;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpEmailSenderAdapterTest {

    private static final String FROM = "nao-responda@escapa.com.br";
    private static final String TO = "ana@email.com";
    private static final String SUBJECT = "Convite para a Escapa!";
    private static final String HTML = "<html><body style=\"color: #000;\"><p>Olá, Ana!</p></body></html>";
    private static final String TEXT = "Olá, Ana!";

    private final CapturingMailSender mailSender = new CapturingMailSender();

    @Test
    void shouldSendTheMessageUnchangedWhenTestModeIsOff() throws Exception {
        new SmtpEmailSenderAdapter(mailSender, FROM, false, "").send(TO, SUBJECT, HTML, TEXT);

        final MimeMessage message = mailSender.single();
        assertEquals(FROM, ((InternetAddress) message.getFrom()[0]).getAddress());
        assertEquals(TO, ((InternetAddress) message.getAllRecipients()[0]).getAddress());
        assertEquals(SUBJECT, message.getSubject());
        assertEquals(HTML, body(message, "text/html"));
        assertEquals(TEXT, body(message, "text/plain"));
    }

    @Test
    void shouldMarkSubjectHtmlAndTextAsTestWhenTestModeIsOn() throws Exception {
        new SmtpEmailSenderAdapter(mailSender, FROM, true, "").send(TO, SUBJECT, HTML, TEXT);

        final MimeMessage message = mailSender.single();
        assertEquals(TO, ((InternetAddress) message.getAllRecipients()[0]).getAddress());
        assertEquals("[TESTE] " + SUBJECT, message.getSubject());
        assertEquals(SmtpEmailSenderAdapter.TEST_NOTICE + "\n\n" + TEXT, body(message, "text/plain"));

        final String html = body(message, "text/html");
        final int bodyTagEnd = html.indexOf("<body style=\"color: #000;\">") + "<body style=\"color: #000;\">".length();
        final int notice = html.indexOf(SmtpEmailSenderAdapter.TEST_NOTICE);
        assertTrue(notice > bodyTagEnd, "the notice goes inside the body");
        assertTrue(notice < html.indexOf("<p>Olá, Ana!</p>"), "the notice goes before the content");
    }

    @Test
    void shouldPutTheNoticeAtTheStartWhenTheHtmlHasNoBodyTag() throws Exception {
        new SmtpEmailSenderAdapter(mailSender, FROM, true, "").send(TO, SUBJECT, "<p>Olá, Ana!</p>", TEXT);

        final String html = body(mailSender.single(), "text/html");
        assertTrue(html.startsWith("<div"));
        assertTrue(html.indexOf(SmtpEmailSenderAdapter.TEST_NOTICE) < html.indexOf("<p>Olá, Ana!</p>"));
    }

    @Test
    void shouldRedirectEveryMessageWhenTestModeIsOnAndTheRedirectIsSet() throws Exception {
        new SmtpEmailSenderAdapter(mailSender, FROM, true, "dev@escapa.com.br").send(TO, SUBJECT, HTML, TEXT);

        final MimeMessage message = mailSender.single();
        assertEquals(1, message.getAllRecipients().length);
        assertEquals("dev@escapa.com.br", ((InternetAddress) message.getAllRecipients()[0]).getAddress());
    }

    @Test
    void shouldIgnoreTheRedirectWhenTestModeIsOff() throws Exception {
        new SmtpEmailSenderAdapter(mailSender, FROM, false, "dev@escapa.com.br").send(TO, SUBJECT, HTML, TEXT);

        assertEquals(TO, ((InternetAddress) mailSender.single().getAllRecipients()[0]).getAddress());
    }

    @Test
    void shouldWrapTheSmtpFailure() {
        final JavaMailSenderImpl failingSender = new JavaMailSenderImpl() {
            @Override
            protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
                throw new MailSendException("SMTP server unavailable");
            }
        };
        final SmtpEmailSenderAdapter adapter = new SmtpEmailSenderAdapter(failingSender, FROM, false, "");

        final EmailSendingException ex = assertThrows(EmailSendingException.class,
                () -> adapter.send(TO, SUBJECT, HTML, TEXT));

        assertEquals("Failed to send e-mail", ex.getMessage());
    }

    /** Corpo da parte com o tipo pedido; o HTML e o texto vão juntos como alternativas. */
    private static String body(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                final String found = body(multipart.getBodyPart(i), mimeType);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Guarda a mensagem em vez de abrir conexão com o SMTP. */
    private static final class CapturingMailSender extends JavaMailSenderImpl {

        private final List<MimeMessage> sent = new ArrayList<>();

        @Override
        protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
            sent.addAll(List.of(mimeMessages));
        }

        MimeMessage single() throws Exception {
            assertEquals(1, sent.size());
            final MimeMessage message = sent.get(0);
            // Gera os cabeçalhos (Content-Type de cada parte), como no envio de verdade.
            message.saveChanges();
            return message;
        }
    }
}
