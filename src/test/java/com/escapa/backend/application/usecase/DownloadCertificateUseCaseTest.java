package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateFile;
import com.escapa.backend.domain.certificate.CertificateGenerationException;
import com.escapa.backend.domain.certificate.CertificateAccessDeniedException;
import com.escapa.backend.domain.certificate.CertificateNotFoundException;
import com.escapa.backend.domain.certificate.CertificateTheme;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadCertificateUseCaseTest {

    private static final String COURSE_TITLE = "Marketing Digital para Hospitalidade";
    private static final UUID STUDENT_ID = UUID.fromString("a0000000-0000-4000-a000-000000000010");

    private final InMemoryCertificateRepositoryPort certificateRepository = new InMemoryCertificateRepositoryPort();
    private final FakeCertificatePdfGeneratorPort pdfGenerator = new FakeCertificatePdfGeneratorPort();
    private final DownloadCertificateUseCase useCase = new DownloadCertificateUseCase(
            new GetCertificateDetailsUseCase(certificateRepository), certificateRepository, pdfGenerator);

    private void givenCertificate(String code, String courseTitle, LocalDate conclusionDate) {
        certificateRepository.addDetails(new CertificateDetails(
                conclusionDate, 960, code,
                new CertificateDetails.Student(STUDENT_ID, "Jorge Amado", null, true),
                new CertificateDetails.Course(
                        UUID.randomUUID(), courseTitle, "Estrategias de marketing", "Marketing",
                        "Intermediario", null, 960, 44, 5.0, 183, "Thamiris Magalhaes", 117.0)));
    }

    private void givenCertificate(String code) {
        givenCertificate(code, COURSE_TITLE, LocalDate.of(2026, 8, 21));
    }

    private static byte[] expectedPdf(String code, CertificateTheme theme, String version) {
        return FakeCertificatePdfGeneratorPort.pdfFor(code, theme, version);
    }

    @Test
    void shouldGeneratePdfInTheLightTheme() {
        givenCertificate("ESC-1");

        final CertificateFile file = useCase.execute(STUDENT_ID, "ESC-1", CertificateTheme.LIGHT);

        assertArrayEquals(expectedPdf("ESC-1", CertificateTheme.LIGHT, "v2"), file.content());
    }

    @Test
    void shouldGeneratePdfInTheDarkTheme() {
        givenCertificate("ESC-1");

        final CertificateFile file = useCase.execute(STUDENT_ID, "ESC-1", CertificateTheme.DARK);

        assertArrayEquals(expectedPdf("ESC-1", CertificateTheme.DARK, "v2"), file.content());
    }

    @Test
    void shouldReuseTheCachedPdfOnTheSecondDownload() {
        givenCertificate("ESC-2");

        final CertificateFile first = useCase.execute(STUDENT_ID, "ESC-2", CertificateTheme.LIGHT);
        final CertificateFile second = useCase.execute(STUDENT_ID, "ESC-2", CertificateTheme.LIGHT);

        assertArrayEquals(first.content(), second.content());
        assertEquals(1, pdfGenerator.calls());
    }

    @Test
    void shouldKeepAnIndependentCachePerTheme() {
        givenCertificate("ESC-3");

        useCase.execute(STUDENT_ID, "ESC-3", CertificateTheme.LIGHT);
        useCase.execute(STUDENT_ID, "ESC-3", CertificateTheme.DARK);
        useCase.execute(STUDENT_ID, "ESC-3", CertificateTheme.LIGHT);
        useCase.execute(STUDENT_ID, "ESC-3", CertificateTheme.DARK);

        assertEquals(2, pdfGenerator.calls());
        assertEquals(2, certificateRepository.cachedPdfCount());
    }

    @Test
    void shouldRegenerateWhenTheTemplateVersionChanges() {
        givenCertificate("ESC-4");
        useCase.execute(STUDENT_ID, "ESC-4", CertificateTheme.LIGHT);

        pdfGenerator.useTemplateVersion("v3");
        final CertificateFile file = useCase.execute(STUDENT_ID, "ESC-4", CertificateTheme.LIGHT);

        assertArrayEquals(expectedPdf("ESC-4", CertificateTheme.LIGHT, "v3"), file.content());
        assertEquals(2, pdfGenerator.calls());
        // O PDF da versao antiga sai do cache junto com a gravacao do novo.
        assertEquals(1, certificateRepository.cachedPdfCount());
    }

    @Test
    void shouldRejectUnknownVerificationCode() {
        assertThrows(CertificateNotFoundException.class,
                () -> useCase.execute(STUDENT_ID, "DOES-NOT-EXIST", CertificateTheme.LIGHT));
        assertEquals(0, pdfGenerator.calls());
    }

    @Test
    void shouldRejectCertificateOfAnUnconcludedCourseEvenWithACachedPdf() {
        givenCertificate("ESC-5", COURSE_TITLE, null);
        certificateRepository.saveCachedPdf(
                "ESC-5", CertificateTheme.LIGHT, "v2", "stale".getBytes(StandardCharsets.UTF_8));

        assertThrows(CertificateNotFoundException.class,
                () -> useCase.execute(STUDENT_ID, "ESC-5", CertificateTheme.LIGHT));
        assertEquals(0, pdfGenerator.calls());
    }

    @Test
    void shouldNotCacheAnythingWhenGenerationFails() {
        givenCertificate("ESC-6");
        pdfGenerator.failWith(new CertificateGenerationException("Chromium crashed", null));

        assertThrows(CertificateGenerationException.class,
                () -> useCase.execute(STUDENT_ID, "ESC-6", CertificateTheme.LIGHT));

        assertTrue(certificateRepository.findCachedPdf("ESC-6", CertificateTheme.LIGHT, "v2").isEmpty());
    }

    @Test
    void shouldGenerateOnlyOnceForSimultaneousFirstDownloads() throws Exception {
        givenCertificate("ESC-7");
        final CountDownLatch release = new CountDownLatch(1);
        pdfGenerator.holdUntil(release);

        final List<Future<CertificateFile>> downloads = downloadConcurrently("ESC-7", CertificateTheme.DARK, 8, release);

        final byte[] expected = expectedPdf("ESC-7", CertificateTheme.DARK, "v2");
        for (Future<CertificateFile> download : downloads) {
            assertArrayEquals(expected, download.get(5, TimeUnit.SECONDS).content());
        }
        assertEquals(1, pdfGenerator.calls());
    }

    @Test
    void shouldPropagateTheFailureToConcurrentDownloadsWithoutCaching() throws Exception {
        givenCertificate("ESC-8");
        final CountDownLatch release = new CountDownLatch(1);
        pdfGenerator.holdUntil(release);
        pdfGenerator.failWith(new CertificateGenerationException("Chromium crashed", null));

        final List<Future<CertificateFile>> downloads = downloadConcurrently("ESC-8", CertificateTheme.LIGHT, 3, release);

        for (Future<CertificateFile> download : downloads) {
            final ExecutionException error =
                    assertThrows(ExecutionException.class, () -> download.get(5, TimeUnit.SECONDS));
            assertInstanceOf(CertificateGenerationException.class, error.getCause());
        }
        assertEquals(1, pdfGenerator.calls());
        assertTrue(certificateRepository.findCachedPdf("ESC-8", CertificateTheme.LIGHT, "v2").isEmpty());
    }

    @Test
    void shouldBuildTheDownloadFilenameFromTheCourseTitle() {
        givenCertificate("ESC-9");

        final CertificateFile file = useCase.execute(STUDENT_ID, "ESC-9", CertificateTheme.LIGHT);

        assertEquals("certificado-marketing-digital-para-hospitalidade.pdf", file.filename());
    }

    @Test
    void shouldStripAccentsAndSpecialCharactersFromTheFilename() {
        givenCertificate("ESC-10", "Gestão de Áreas & Operações!", LocalDate.of(2026, 8, 21));

        final CertificateFile file = useCase.execute(STUDENT_ID, "ESC-10", CertificateTheme.LIGHT);

        assertEquals("certificado-gestao-de-areas-operacoes.pdf", file.filename());
    }

    /**
     * Dispara {@code requests} downloads ao mesmo tempo enquanto o gerador esta
     * segurado, e so libera a geracao depois que todos chegaram.
     */
    private List<Future<CertificateFile>> downloadConcurrently(
            String code, CertificateTheme theme, int requests, CountDownLatch release) throws InterruptedException {
        final ExecutorService pool = Executors.newFixedThreadPool(requests);
        final CountDownLatch started = new CountDownLatch(requests);
        final List<Future<CertificateFile>> downloads = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            downloads.add(pool.submit(() -> {
                started.countDown();
                return useCase.execute(STUDENT_ID, code, theme);
            }));
        }
        assertTrue(started.await(5, TimeUnit.SECONDS));
        // Margem para as requisicoes passarem da consulta ao cache e chegarem a reserva da chave.
        TimeUnit.MILLISECONDS.sleep(200);
        release.countDown();
        pool.shutdown();
        return downloads;
    }

    @Test
    void shouldRejectDownloadWhenTheUserIsNotTheCertificateOwnerEvenWithACachedPdf() {
        givenCertificate("ESC-11");
        certificateRepository.saveCachedPdf(
                "ESC-11", CertificateTheme.LIGHT, "v2", "cached".getBytes(StandardCharsets.UTF_8));

        assertThrows(CertificateAccessDeniedException.class,
                () -> useCase.execute(UUID.randomUUID(), "ESC-11", CertificateTheme.LIGHT));
        assertEquals(0, pdfGenerator.calls());
    }
}
