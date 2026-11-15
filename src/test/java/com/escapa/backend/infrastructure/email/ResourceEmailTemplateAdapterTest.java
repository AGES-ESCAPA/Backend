package com.escapa.backend.infrastructure.email;

import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceEmailTemplateAdapterTest {

    private static final Map<String, String> VARIABLES = Map.of(
            "nome", "Ana",
            "empresa", "Silva & Filhos <ME>",
            "link", "http://localhost:3000/convite?token=abc&origem=email");

    private final ResourceEmailTemplateAdapter adapter = new ResourceEmailTemplateAdapter();

    @Test
    void shouldReplaceTheMarkersAndEscapeTheValuesInTheHtmlTemplate() {
        final String html = adapter.render("convite-colaborador.html", VARIABLES);

        assertTrue(html.contains("Olá, Ana!"));
        assertTrue(html.contains("Silva &amp; Filhos &lt;ME&gt;"));
        assertTrue(html.contains("href=\"http://localhost:3000/convite?token=abc&amp;origem=email\""));
        assertFalse(html.contains("{{"));
    }

    @Test
    void shouldReplaceTheMarkersWithTheRawValuesInTheTextTemplate() {
        final String text = adapter.render("convite-colaborador.txt", VARIABLES);

        assertTrue(text.contains("Olá, Ana!"));
        assertTrue(text.contains("Silva & Filhos <ME>"));
        assertTrue(text.contains("http://localhost:3000/convite?token=abc&origem=email"));
        assertFalse(text.contains("{{"));
    }

    @Test
    void shouldKeepTheTestNoticeOutOfTheTemplates() {
        final String html = adapter.render("convite-colaborador.html", VARIABLES);
        final String text = adapter.render("convite-colaborador.txt", VARIABLES);

        assertFalse(html.contains("TESTE"));
        assertFalse(text.contains("TESTE"));
    }

    @Test
    void shouldFailWhenTheTemplateDoesNotExist() {
        assertThrows(UncheckedIOException.class, () -> adapter.render("nao-existe.html", Map.of()));
    }
}
