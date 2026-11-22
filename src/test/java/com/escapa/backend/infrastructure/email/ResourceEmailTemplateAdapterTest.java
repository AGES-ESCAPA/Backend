package com.escapa.backend.infrastructure.email;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ResourceEmailTemplateAdapterTest {

    private static final Map<String, String> VARIABLES = Map.of(
            "titulo", "Convite para a Escapa!",
            "preheader", "Você foi convidado(a) para acessar os cursos.",
            "nome", "Ana",
            "empresa", "Silva & Filhos <ME>",
            "link", "http://localhost:3000/convite?token=abc&origem=email");

    private final ResourceEmailTemplateAdapter adapter = new ResourceEmailTemplateAdapter();

    @Test
    void shouldWrapTheHtmlContentInTheBrandLayout() {
        final String html = adapter.render("convite-colaborador.html", VARIABLES);

        assertTrue(html.contains("cid:escapa-logo"));
        assertTrue(html.contains("background-color:#0c0c1a"));
        assertTrue(html.contains("background-color:#5be5ff"));
        assertTrue(html.contains("Equipe Escapa!"));
        assertTrue(html.contains("<title>Convite para a Escapa!</title>"));
        // Variáveis HTML passam por HtmlUtils: acentos viram entidades (ê → &ecirc;).
        assertTrue(html.contains("Voc&ecirc; foi convidado(a) para acessar os cursos."));
    }

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
        assertFalse(text.contains("cid:escapa-logo"));
    }

    @Test
    void shouldKeepTheTestNoticeOutOfTheTemplates() {
        final String html = adapter.render("convite-colaborador.html", VARIABLES);
        final String text = adapter.render("convite-colaborador.txt", VARIABLES);

        assertFalse(html.contains("TESTE"));
        assertFalse(text.contains("TESTE"));
    }

    @Test
    void shouldApplyTheBrandLayoutToEveryExistingHtmlEmail() {
        final Map<String, String> invite = VARIABLES;
        final Map<String, String> reset = Map.of(
                "titulo", "Redefinir senha — Escapa!",
                "preheader", "Recebemos um pedido para redefinir a senha.",
                "nome", "Ana",
                "link", "http://localhost:3000/redefinir-senha?token=abc");
        final Map<String, String> emailChange = Map.of(
                "titulo", "Confirme seu novo e-mail — Escapa!",
                "preheader", "Confirme a alteração do e-mail da sua conta.",
                "nome", "Ana",
                "novoEmail", "novo@email.com",
                "link", "http://localhost:3000/confirmar-email?token=abc");

        for (final String html : new String[] {
                adapter.render("convite-colaborador.html", invite),
                adapter.render("recuperacao-senha.html", reset),
                adapter.render("confirmacao-email.html", emailChange)
        }) {
            assertTrue(html.contains("cid:escapa-logo"));
            assertTrue(html.contains("background-color:#0c0c1a"));
            assertTrue(html.contains("background-color:#2b6fe7"));
            assertTrue(html.contains("Equipe Escapa!"));
            assertFalse(html.contains("{{"));
        }
    }

    @Test
    void shouldFailWhenTheTemplateDoesNotExist() {
        assertThrows(UncheckedIOException.class, () -> adapter.render("nao-existe.html", Map.of()));
    }
}
