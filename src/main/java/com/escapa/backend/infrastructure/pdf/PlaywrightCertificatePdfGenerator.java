package com.escapa.backend.infrastructure.pdf;

import com.escapa.backend.application.model.FormattedCertificate;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.escapa.backend.domain.certificate.CertificateGenerationException;
import com.escapa.backend.domain.certificate.CertificateTheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.util.Locale;

/**
 * PDF do certificado fiel a tela (US-18): o template Thymeleaf
 * {@code templates/certificate/certificate.html} e impresso pelo Chromium.
 */
@Component
public class PlaywrightCertificatePdfGenerator implements CertificatePdfGeneratorPort {

    private static final String TEMPLATE = "certificate/certificate";
    // Acima desses tamanhos o nome e o curso ganham uma fonte menor, para caberem
    // em ate duas ou tres linhas sem empurrar o rodape para fora da pagina.
    private static final int LONG_STUDENT_NAME = 28;
    private static final int VERY_LONG_STUDENT_NAME = 48;
    private static final int LONG_COURSE_TITLE = 48;
    private static final int VERY_LONG_COURSE_TITLE = 80;

    private final ITemplateEngine templateEngine;
    private final CertificateAssets assets;
    private final CertificatePdfRenderer renderer;
    private final String templateVersion;

    public PlaywrightCertificatePdfGenerator(
            ITemplateEngine templateEngine,
            CertificateAssets assets,
            CertificatePdfRenderer renderer,
            @Value("${certificate.template.version}") String templateVersion
    ) {
        this.templateEngine = templateEngine;
        this.assets = assets;
        this.renderer = renderer;
        this.templateVersion = templateVersion;
    }

    @Override
    public String templateVersion() {
        return templateVersion;
    }

    @Override
    public byte[] generate(FormattedCertificate certificate, CertificateTheme theme) {
        return renderer.render(renderHtml(certificate, theme));
    }

    /** HTML final, com CSS, fontes e logo embutidos. */
    String renderHtml(FormattedCertificate certificate, CertificateTheme theme) {
        final Context context = new Context(Locale.of("pt", "BR"));
        context.setVariable("certificate", certificate);
        context.setVariable("theme", theme.param());
        context.setVariable("stylesheet", assets.stylesheet());
        context.setVariable("logo", assets.logo(theme));
        final String studentNameSize = sizeClass(certificate.studentName(), LONG_STUDENT_NAME, VERY_LONG_STUDENT_NAME);
        final String courseTitleSize = sizeClass(certificate.courseTitle(), LONG_COURSE_TITLE, VERY_LONG_COURSE_TITLE);
        context.setVariable("studentNameSize", studentNameSize);
        context.setVariable("courseTitleSize", courseTitleSize);
        context.setVariable("compactLayout", !studentNameSize.isEmpty() || !courseTitleSize.isEmpty());
        try {
            return templateEngine.process(TEMPLATE, context);
        } catch (TemplateProcessingException e) {
            throw new CertificateGenerationException("Failed to render the certificate template", e);
        }
    }

    private static String sizeClass(String text, int longLength, int veryLongLength) {
        if (text.length() > veryLongLength) {
            return "is-very-long";
        }
        return text.length() > longLength ? "is-long" : "";
    }
}
