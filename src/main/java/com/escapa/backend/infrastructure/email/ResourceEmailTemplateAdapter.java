package com.escapa.backend.infrastructure.email;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.util.HtmlUtils;

import com.escapa.backend.application.port.EmailTemplatePort;

/**
 * Lê os templates de {@code src/main/resources/email/} e troca cada {@code {{chave}}}
 * pelo valor. Nos templates {@code .html} o valor é escapado, para um nome com
 * {@code <} ou {@code &} não quebrar (nem injetar) HTML no e-mail.
 *
 * <p>Templates HTML de conteúdo (qualquer um que não seja {@code layout.html})
 * são envelopados pelo layout visual da Escapa!, que define cabeçalho, cartão,
 * rodapé e identidade de marca. O marcador {@code {{conteudo}}} do layout recebe
 * o HTML já renderizado do template pedido — sem novo escape.
 */
public class ResourceEmailTemplateAdapter implements EmailTemplatePort {

    private static final String BASE = "email/";
    private static final String LAYOUT = "layout.html";
    private static final String DEFAULT_TITLE = "Escapa!";
    private static final String DEFAULT_PREHEADER = "Mensagem da Escapa!";

    @Override
    public String render(String templateName, Map<String, String> variables) {
        final boolean html = templateName.endsWith(".html");
        final String content = applyVariables(read(templateName), variables, html);

        if (!html || LAYOUT.equals(templateName)) {
            return content;
        }

        final Map<String, String> layoutVariables = new HashMap<>(variables);
        layoutVariables.putIfAbsent("titulo", DEFAULT_TITLE);
        layoutVariables.putIfAbsent("preheader", DEFAULT_PREHEADER);

        // O conteúdo já foi escapado acima; entra no layout sem novo escape.
        final String layout = applyVariables(read(LAYOUT), layoutVariables, true);
        return layout.replace("{{conteudo}}", content);
    }

    private static String applyVariables(String template, Map<String, String> variables, boolean htmlEscape) {
        String content = template;
        for (final Map.Entry<String, String> variable : variables.entrySet()) {
            if ("conteudo".equals(variable.getKey())) {
                // Reservado ao envelope do layout; não vem dos mailers.
                continue;
            }
            final String value = htmlEscape ? HtmlUtils.htmlEscape(variable.getValue()) : variable.getValue();
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
