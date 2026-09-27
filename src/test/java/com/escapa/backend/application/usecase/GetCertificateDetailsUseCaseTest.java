package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateView;
import com.escapa.backend.domain.certificate.CertificateNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetCertificateDetailsUseCaseTest {

    private final InMemoryCertificateRepositoryPort certificateRepository = new InMemoryCertificateRepositoryPort();
    private final GetCertificateDetailsUseCase useCase = new GetCertificateDetailsUseCase(certificateRepository);

    private CertificateDetails givenCertificate(String code, LocalDate conclusionDate) {
        final CertificateDetails details = new CertificateDetails(
                conclusionDate, 960, code,
                new CertificateDetails.Student("Jorge Amado", null, true),
                new CertificateDetails.Course(
                        UUID.randomUUID(), "Marketing Digital para Hospitalidade", "Estrategias de marketing",
                        "Marketing", "Intermediario", "https://cdn.escapa.com.br/thumb.jpg",
                        960, 44, 5.0, 183, "Thamiris Magalhaes", 117.0));
        certificateRepository.addDetails(details);
        return details;
    }

    @Test
    void shouldReturnTheCertificateDetailsOfAConcludedCourse() {
        final CertificateDetails details = givenCertificate("ESC-1", LocalDate.of(2026, 8, 21));

        final CertificateView view = useCase.execute("ESC-1");

        assertSame(details, view.details());
    }

    @Test
    void shouldReturnOwnerAsFalseUntilAuthenticationExists() {
        givenCertificate("ESC-2", LocalDate.of(2026, 8, 21));

        assertFalse(useCase.execute("ESC-2").owner());
    }

    @Test
    void shouldRejectUnknownVerificationCode() {
        final CertificateNotFoundException ex =
                assertThrows(CertificateNotFoundException.class, () -> useCase.execute("DOES-NOT-EXIST"));

        assertEquals("Certificate not found: DOES-NOT-EXIST", ex.getMessage());
    }

    @Test
    void shouldRejectCertificateOfAnUnconcludedCourse() {
        givenCertificate("ESC-3", null);

        assertThrows(CertificateNotFoundException.class, () -> useCase.execute("ESC-3"));
    }
}
