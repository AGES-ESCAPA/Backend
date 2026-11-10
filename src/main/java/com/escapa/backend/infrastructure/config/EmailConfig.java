package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.EmailSenderPort;
import com.escapa.backend.application.port.EmailTemplatePort;
import com.escapa.backend.infrastructure.email.ResourceEmailTemplateAdapter;
import com.escapa.backend.infrastructure.email.SmtpEmailSenderAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/** Beans do envio de e-mail (US-31). O JavaMailSender vem do Spring Boot, a partir de spring.mail.*. */
@Configuration
public class EmailConfig {

    @Bean
    public EmailSenderPort emailSenderPort(
            JavaMailSender javaMailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.test-mode}") boolean testMode,
            @Value("${app.mail.test-redirect-to}") String testRedirectTo
    ) {
        return new SmtpEmailSenderAdapter(javaMailSender, from, testMode, testRedirectTo);
    }

    @Bean
    public EmailTemplatePort emailTemplatePort() {
        return new ResourceEmailTemplateAdapter();
    }
}
