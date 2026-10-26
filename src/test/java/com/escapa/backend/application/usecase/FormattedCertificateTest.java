package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.FormattedCertificate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormattedCertificateTest {

    @Test
    void shouldFormatEveryFieldShownOnTheCertificate() {
        final CertificateDetails details = new CertificateDetails(
                LocalDate.of(2025, 8, 21), 960, "ESC-21AGO25-7X9L2M3N",
                new CertificateDetails.Student(UUID.randomUUID(), "Jorge Amado", null, true),
                new CertificateDetails.Course(
                        UUID.randomUUID(), "Marketing Digital para Hospitalidade", null, null, null, null,
                        960, 44, null, 0, null, null));

        final FormattedCertificate formatted = FormattedCertificate.from(details);

        assertEquals("Jorge Amado", formatted.studentName());
        assertEquals("Marketing Digital para Hospitalidade", formatted.courseTitle());
        assertEquals("21 de agosto de 2025", formatted.conclusionDate());
        assertEquals("16 horas", formatted.workload());
        assertEquals("ESC-21AGO25-7X9L2M3N", formatted.verificationCode());
        assertEquals("escapa.com.br/certificados/ESC-21AGO25-7X9L2M3N", formatted.validationUrl());
    }

    @Test
    void shouldWriteTheDayWithoutALeadingZero() {
        assertEquals("1 de março de 2026", FormattedCertificate.formatConclusionDate(LocalDate.of(2026, 3, 1)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "960 | 16 horas",
        "60  | 1 hora",
        "90  | 1 hora e 30 minutos",
        "121 | 2 horas e 1 minuto",
        "45  | 45 minutos",
        "1   | 1 minuto",
        "0   | 0 horas",
    })
    void shouldWriteTheWorkloadInFull(int minutes, String expected) {
        assertEquals(expected, FormattedCertificate.formatWorkload(minutes));
    }

    @Test
    void shouldTreatAMissingWorkloadAsZeroHours() {
        assertEquals("0 horas", FormattedCertificate.formatWorkload(null));
    }
}
