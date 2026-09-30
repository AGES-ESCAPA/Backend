package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateFile;
import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.domain.certificate.CertificateNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DownloadCertificateUseCaseTest {

    private final InMemoryCertificateRepositoryPort certificateRepository = new InMemoryCertificateRepositoryPort();
    private final FakeCertificatePdfGeneratorPort pdfGenerator = new FakeCertificatePdfGeneratorPort();
    private final DownloadCertificateUseCase useCase =
            new DownloadCertificateUseCase(certificateRepository, pdfGenerator);

    private CertificateRecord givenCertificate(String code, String courseTitle, byte[] cachedPdf) {
        final CertificateRecord certificate = new CertificateRecord(
                UUID.randomUUID(), UUID.randomUUID(), "Jorge Amado", courseTitle,
                960, LocalDate.of(2026, 8, 21), code, cachedPdf);
        certificateRepository.add(certificate);
        return certificate;
    }

    @Test
    void shouldGenerateAndCachePdfOnFirstDownload() {
        givenCertificate("ESC-1", "Marketing Digital para Hospitalidade", null);

        final CertificateFile file = useCase.execute("ESC-1");

        assertArrayEquals("pdf-of-ESC-1".getBytes(), file.content());
        assertEquals(1, pdfGenerator.calls());
    }

    @Test
    void shouldReuseCachedPdfWithoutRegenerating() {
        final byte[] cached = "already-generated".getBytes();
        givenCertificate("ESC-2", "Marketing Digital para Hospitalidade", cached);

        final CertificateFile file = useCase.execute("ESC-2");

        assertArrayEquals(cached, file.content());
        assertEquals(0, pdfGenerator.calls());
    }

    @Test
    void shouldRejectUnknownVerificationCode() {
        assertThrows(CertificateNotFoundException.class, () -> useCase.execute("DOES-NOT-EXIST"));
    }

    @Test
    void shouldRejectCertificateOfAnUnconcludedCourse() {
        final CertificateRecord certificate = new CertificateRecord(
                UUID.randomUUID(), UUID.randomUUID(), "Jorge Amado", "Curso Incompleto",
                960, null, "ESC-3", null);
        certificateRepository.add(certificate);

        assertThrows(CertificateNotFoundException.class, () -> useCase.execute("ESC-3"));
        assertEquals(0, pdfGenerator.calls());
    }

    @Test
    void shouldBuildTheDownloadFilenameFromTheCourseTitle() {
        givenCertificate("ESC-4", "Marketing Digital para Hospitalidade", null);

        final CertificateFile file = useCase.execute("ESC-4");

        assertEquals("certificado-marketing-digital-para-hospitalidade.pdf", file.filename());
    }

    @Test
    void shouldStripAccentsAndSpecialCharactersFromTheFilename() {
        givenCertificate("ESC-5", "Gestão de Áreas & Operações!", null);

        final CertificateFile file = useCase.execute("ESC-5");

        assertEquals("certificado-gestao-de-areas-operacoes.pdf", file.filename());
    }
}
