package com.escapa.backend.infrastructure.pdf;

import com.escapa.backend.application.model.FormattedCertificate;
import com.escapa.backend.domain.certificate.CertificateGenerationException;
import com.escapa.backend.domain.certificate.CertificateTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.exceptions.TemplateProcessingException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaywrightCertificatePdfGeneratorUnitTest {

    private ITemplateEngine templateEngine;
    private CertificatePdfRenderer renderer;
    private PlaywrightCertificatePdfGenerator generator;

    @BeforeEach
    void setUp() {
        this.templateEngine = mock(ITemplateEngine.class);
        this.renderer = mock(CertificatePdfRenderer.class);
        this.generator = new PlaywrightCertificatePdfGenerator(
                templateEngine, new CertificateAssets(), renderer, "test-version");
    }

    @Test
    void shouldRenderShortNamesWithoutCompactLayout() {
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>certificate</html>");
        final String html = generator.renderHtml(certificate("Ana", "Curso"), CertificateTheme.DARK);
        final ArgumentCaptor<IContext> context = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(eq("certificate/certificate"), context.capture());

        assertEquals("<html>certificate</html>", html);
        assertEquals("", context.getValue().getVariable("studentNameSize"));
        assertEquals("", context.getValue().getVariable("courseTitleSize"));
        assertFalse((Boolean) context.getValue().getVariable("compactLayout"));
    }

    @Test
    void shouldUseCompactClassesForLongAndVeryLongNames() {
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>certificate</html>");

        generator.renderHtml(certificate("n".repeat(29), "c".repeat(49)), CertificateTheme.LIGHT);
        final ArgumentCaptor<IContext> longContext = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(eq("certificate/certificate"), longContext.capture());
        assertEquals("is-long", longContext.getValue().getVariable("studentNameSize"));
        assertEquals("is-long", longContext.getValue().getVariable("courseTitleSize"));
        assertTrue((Boolean) longContext.getValue().getVariable("compactLayout"));

        generator.renderHtml(certificate("n".repeat(49), "c".repeat(81)), CertificateTheme.LIGHT);
        final ArgumentCaptor<IContext> veryLongContext = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine, org.mockito.Mockito.times(2))
                .process(eq("certificate/certificate"), veryLongContext.capture());
        assertEquals("is-very-long", veryLongContext.getAllValues().get(1).getVariable("studentNameSize"));
        assertEquals("is-very-long", veryLongContext.getAllValues().get(1).getVariable("courseTitleSize"));
    }

    @Test
    void shouldPassRenderedHtmlToThePdfRenderer() {
        final byte[] expected = {1, 2, 3};
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("<html>certificate</html>");
        when(renderer.render("<html>certificate</html>")).thenReturn(expected);

        assertArrayEquals(expected, generator.generate(certificate("Ana", "Curso"), CertificateTheme.DARK));
        assertEquals("test-version", generator.templateVersion());
        verify(renderer).render("<html>certificate</html>");
    }

    @Test
    void shouldWrapTemplateProcessingFailures() {
        when(templateEngine.process(anyString(), any(IContext.class)))
                .thenThrow(new TemplateProcessingException("template failure"));

        assertThrows(CertificateGenerationException.class,
                () -> generator.renderHtml(certificate("Ana", "Curso"), CertificateTheme.DARK));
    }

    private FormattedCertificate certificate(String studentName, String courseTitle) {
        return new FormattedCertificate(studentName, courseTitle, "22 de outubro de 2026",
                "1 hora", "ESC-TEST", "escapa.com.br/certificados/ESC-TEST");
    }
}
