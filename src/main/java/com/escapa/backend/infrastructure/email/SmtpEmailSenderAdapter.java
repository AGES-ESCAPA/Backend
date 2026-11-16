package com.escapa.backend.infrastructure.email;

import com.escapa.backend.application.port.EmailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Envia o e-mail pelo SMTP configurado em {@code spring.mail.*}. Com o modo de teste
 * ({@code app.mail.test-mode}) ligado, marca a mensagem como teste: prefixo no assunto e
 * aviso no topo do HTML e do texto. O aviso fica só aqui, nunca nos templates, para não
 * vazar para produção. Desligado, a mensagem sai exatamente como o caso de uso montou.
 */
public class SmtpEmailSenderAdapter implements EmailSenderPort {

    static final String TEST_SUBJECT_PREFIX = "[TESTE] ";
    static final String TEST_NOTICE = "⚠️ Este é um e-mail de TESTE enviado pelo ambiente de desenvolvimento."
            + " Não é uma mensagem real.";
    private static final String TEST_NOTICE_HTML = "<div style=\"background:#fff3cd;color:#664d03;"
            + "border:1px solid #ffda6a;padding:12px;margin-bottom:16px;font-family:sans-serif;\">"
            + TEST_NOTICE + "</div>";
    private static final Pattern BODY_TAG = Pattern.compile("<body[^>]*>", Pattern.CASE_INSENSITIVE);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean testMode;
    private final String testRedirectTo;

    /**
     * @param testRedirectTo em modo de teste, se preenchido, recebe todos os e-mails no lugar
     *                       do destinatário original; ignorado fora do modo de teste
     */
    public SmtpEmailSenderAdapter(JavaMailSender mailSender, String from, boolean testMode, String testRedirectTo) {
        this.mailSender = mailSender;
        this.from = from;
        this.testMode = testMode;
        this.testRedirectTo = testRedirectTo;
    }

    @Override
    public void send(String to, String subject, String htmlBody, String textBody) {
        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient(to));
            helper.setSubject(testMode ? TEST_SUBJECT_PREFIX + subject : subject);
            helper.setText(
                    testMode ? TEST_NOTICE + "\n\n" + textBody : textBody,
                    testMode ? withHtmlNotice(htmlBody) : htmlBody);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailSendingException("Failed to send e-mail", e);
        }
    }

    private String recipient(String to) {
        final boolean redirect = testMode && testRedirectTo != null && !testRedirectTo.isBlank();
        return redirect ? testRedirectTo : to;
    }

    /** Coloca o aviso logo depois da tag {@code <body>}, ou no início quando o HTML não tem body. */
    private static String withHtmlNotice(String html) {
        final Matcher body = BODY_TAG.matcher(html);
        if (body.find()) {
            return html.substring(0, body.end()) + TEST_NOTICE_HTML + html.substring(body.end());
        }
        return TEST_NOTICE_HTML + html;
    }
}
