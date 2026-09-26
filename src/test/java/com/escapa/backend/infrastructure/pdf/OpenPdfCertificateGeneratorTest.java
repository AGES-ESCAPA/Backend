package com.escapa.backend.infrastructure.pdf;

import com.escapa.backend.application.model.CertificateRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OpenPdfCertificateGeneratorTest {

    private final OpenPdfCertificateGenerator generator = new OpenPdfCertificateGenerator();

    private CertificateRecord givenCertificate(Integer workloadMinutes) {
        return new CertificateRecord(
                UUID.randomUUID(), UUID.randomUUID(), "Jorge Amado", "Marketing Digital para Hospitalidade",
                workloadMinutes, LocalDate.of(2026, 8, 21), "ESC-2026-MKT-0001", null);
    }

    @Test
    void shouldProduceAValidPdfFile() {
        final byte[] pdf = generator.generate(givenCertificate(960));

        assertTrue(pdf.length > 0);
        // Todo PDF comeca com essa assinatura ("%PDF"); confirma que e um arquivo real,
        // sem precisar de um leitor de PDF so para o teste.
        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"));
    }

    @Test
    void shouldNotFailWithAnUnknownWorkload() {
        assertDoesNotThrow(() -> generator.generate(givenCertificate(null)));
        assertDoesNotThrow(() -> generator.generate(givenCertificate(0)));
    }

    @Test
    void shouldNotFailWithAWorkloadThatIsNotAWholeNumberOfHours() {
        assertDoesNotThrow(() -> generator.generate(givenCertificate(90)));
    }
}
