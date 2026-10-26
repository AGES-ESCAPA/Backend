package com.escapa.backend.infrastructure.pdf;

import com.escapa.backend.adapters.dto.CertificateDetailsResponse;
import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateView;
import com.escapa.backend.application.model.FormattedCertificate;
import com.escapa.backend.domain.certificate.CertificateTheme;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gera o PDF com o Chromium de verdade. Fica fora do `mvn verify` padrao pela
 * tag "pdf"; rode com {@code mvn test -Dsurefire.excludedGroups= -Dgroups=pdf}
 * depois de instalar o navegador (ver README).
 *
 * Cada teste tambem salva o PDF e uma imagem dele em target/certificate-preview,
 * para a comparacao lado a lado com a tela.
 */
@Tag("pdf")
class PlaywrightCertificatePdfGeneratorTest {

    // A4 horizontal em pontos (1 pt = 1/72 pol): 841,89 x 595,28.
    private static final float A4_LANDSCAPE_WIDTH = 842f;
    private static final float A4_LANDSCAPE_HEIGHT = 595f;
    private static final float SIZE_TOLERANCE = 2f;
    // Moldura recuada 1,4% da largura da pagina (certificate.css). A linha de base do
    // texto mais baixo precisa ficar pelo menos 2% da largura acima dela, para as
    // letras (e as descendentes) nao encostarem na moldura.
    private static final float FRAME_INSET = A4_LANDSCAPE_WIDTH * 0.014f;
    private static final float TEXT_CLEARANCE = A4_LANDSCAPE_WIDTH * 0.02f;
    private static final float PREVIEW_DPI = 110f;
    private static final Path PREVIEW_DIR = Path.of("target", "certificate-preview");

    private static CertificatePdfRenderer renderer;
    private static PlaywrightCertificatePdfGenerator generator;

    @BeforeAll
    static void setUp() throws IOException {
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        // Mesmo motor da aplicacao (expressoes em SpEL), sem subir o contexto do Spring.
        final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        renderer = new CertificatePdfRenderer(20, 60, false);
        generator = new PlaywrightCertificatePdfGenerator(templateEngine, new CertificateAssets(), renderer, "v2");
        Files.createDirectories(PREVIEW_DIR);
    }

    @AfterAll
    static void tearDown() {
        renderer.shutdown();
    }

    private static CertificateDetails details(String studentName, String courseTitle) {
        return new CertificateDetails(
                LocalDate.of(2025, 8, 21), 960, "ESC-21AGO25-7X9L2M3N",
                new CertificateDetails.Student(UUID.randomUUID(), studentName, null, true),
                new CertificateDetails.Course(
                        UUID.randomUUID(), courseTitle, "Estrategias de marketing", "Marketing",
                        "Intermediario", null, 960, 44, 5.0, 183, "Thamiris Magalhaes", 117.0));
    }

    @ParameterizedTest
    @EnumSource(CertificateTheme.class)
    void shouldGenerateASingleA4LandscapePage(CertificateTheme theme) throws IOException {
        final byte[] pdf = generate(details("Jorge Amado", "Marketing Digital para Hospitalidade"), theme, "");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
            final PDRectangle page = document.getPage(0).getMediaBox();
            assertEquals(A4_LANDSCAPE_WIDTH, page.getWidth(), SIZE_TOLERANCE);
            assertEquals(A4_LANDSCAPE_HEIGHT, page.getHeight(), SIZE_TOLERANCE);
        }
    }

    @ParameterizedTest
    @EnumSource(CertificateTheme.class)
    void shouldPrintTheSameSelectableTextAsTheCertificateScreen(CertificateTheme theme) throws IOException {
        final CertificateDetails details = details("Jorge Amado", "Marketing Digital para Hospitalidade");
        // Os textos esperados vem da resposta da tela (US-18), nao do gerador.
        final CertificateDetailsResponse screen = CertificateDetailsResponse.from(new CertificateView(details, false));

        final String text = extractText(generate(details, theme, ""));

        assertTrue(text.contains(screen.student().name()), text);
        assertTrue(text.contains(screen.course().title()), text);
        assertTrue(text.contains(screen.certificate().formattedConclusionDate()), text);
        assertTrue(text.contains(screen.certificate().formattedWorkload()), text);
        assertTrue(text.contains(screen.certificate().verificationCode()), text);
        assertTrue(text.contains(screen.certificate().validationUrl()), text);
    }

    @ParameterizedTest
    @EnumSource(CertificateTheme.class)
    void shouldKeepLongNamesAndTitlesOnASinglePage(CertificateTheme theme) throws IOException {
        final CertificateDetails details = details(
                "Maria Aparecida dos Santos Albuquerque de Oliveira Cavalcanti Pereira",
                "Gestao Estrategica de Experiencias em Hotelaria de Luxo, Turismo Regenerativo "
                        + "e Hospitalidade Sustentavel para Destinos Emergentes");

        final byte[] pdf = generate(details, theme, "-long");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
        }
        final float lowestText = lowestTextBottom(pdf);
        assertTrue(lowestText < A4_LANDSCAPE_HEIGHT - FRAME_INSET - TEXT_CLEARANCE,
                "Text crosses the certificate frame at y=" + lowestText);
        final String text = extractText(pdf);
        assertTrue(text.contains("Maria Aparecida dos Santos Albuquerque"), text);
        assertTrue(text.contains("Destinos Emergentes"), text);
        assertTrue(text.contains("escapa.com.br/certificados/ESC-21AGO25-7X9L2M3N"), text);
    }

    @Test
    void shouldEscapeHtmlComingFromTheDatabase() {
        final String html = generator.renderHtml(FormattedCertificate.from(
                details("<b>Jorge</b><script>alert(1)</script>", "Curso & Cia")), CertificateTheme.DARK);

        assertTrue(html.contains("&lt;b&gt;Jorge&lt;/b&gt;&lt;script&gt;"));
        assertTrue(html.contains("Curso &amp; Cia"));
    }

    private static byte[] generate(CertificateDetails details, CertificateTheme theme, String previewSuffix)
            throws IOException {
        final byte[] pdf = generator.generate(FormattedCertificate.from(details), theme);
        savePreview(pdf, theme.param() + previewSuffix);
        return pdf;
    }

    private static String extractText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Quebras de linha do layout viram espaco, para comparar frases inteiras.
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    /** Linha de base do texto horizontal mais baixo da pagina, em pontos a partir do topo. */
    private static float lowestTextBottom(byte[] pdf) throws IOException {
        final float[] lowest = {0f};
        final PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                for (TextPosition position : positions) {
                    // So texto horizontal: as letras do selo seguem o arco, giradas, e
                    // o PDFBox mede essas posicoes em outro referencial.
                    if (position.getDir() == 0) {
                        lowest[0] = Math.max(lowest[0], position.getYDirAdj());
                    }
                }
            }
        };
        try (PDDocument document = Loader.loadPDF(pdf)) {
            stripper.getText(document);
        }
        return lowest[0];
    }

    private static void savePreview(byte[] pdf, String name) throws IOException {
        Files.write(PREVIEW_DIR.resolve("certificado-" + name + ".pdf"), pdf);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, PREVIEW_DPI), "png",
                    PREVIEW_DIR.resolve("certificado-" + name + ".png").toFile());
        }
    }
}
