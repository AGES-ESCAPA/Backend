package com.escapa.backend.infrastructure.email;

import com.escapa.backend.application.port.EmailTemplatePort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Lê os templates de {@code src/main/resources/email/} e troca cada {@code {{chave}}}
 * pelo valor. Nos templates {@code .html} o valor é escapado, para um nome com
 * {@code <} ou {@code &} não quebrar (nem injetar) HTML no e-mail.
 */
public class ResourceEmailTemplateAdapter implements EmailTemplatePort {

    private static final String BASE = "email/";

    @Override
    public String render(String templateName, Map<String, String> variables) {
        final boolean html = templateName.endsWith(".html");
        String content = read(templateName);
        for (final Map.Entry<String, String> variable : variables.entrySet()) {
            final String value = html ? HtmlUtils.htmlEscape(variable.getValue()) : variable.getValue();
            content = content.replace("{{" + variable.getKey() + "}}", value);
        }
        return content;
    }

    private static String read(String templateName) {
        try (InputStream input = new ClassPathResource(BASE + templateName).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("E-mail template not found: " + templateName, e);
        }
    }
}
