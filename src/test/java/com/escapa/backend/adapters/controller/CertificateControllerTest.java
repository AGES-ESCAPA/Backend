package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.application.usecase.DownloadCertificateUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP da US-19: o caso de uso real roda sobre portas em memoria. */
class CertificateControllerTest {

    private static final String URL = "/api/v1/certificates/{verificationCode}/download";
    private static final String ISSUED_CODE = "ESC-2026-MKT-0001";
    private static final byte[] GENERATED_PDF = "%PDF-1.4 fake content".getBytes(StandardCharsets.UTF_8);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final CertificateRecord certificate = new CertificateRecord(
                UUID.randomUUID(), UUID.randomUUID(), "Jorge Amado", "Marketing Digital para Hospitalidade",
                960, LocalDate.of(2026, 8, 21), ISSUED_CODE, null);

        final CertificateRepositoryPort certificateRepositoryPort =
                new CertificateRepositoryPort() {
                    @Override
                    public Optional<CertificateRecord> findByVerificationCode(String verificationCode) {
                        return Optional.of(certificate).filter(c -> c.verificationCode().equals(verificationCode));
                    }

                    @Override
                    public void saveCachedPdf(UUID userId, UUID courseId, byte[] pdf) {
                        // Sem cache real nesse teste: o generator fake sempre devolve o mesmo PDF.
                    }
                };
        final CertificatePdfGeneratorPort certificatePdfGeneratorPort = c -> GENERATED_PDF;

        final DownloadCertificateUseCase useCase =
                new DownloadCertificateUseCase(certificateRepositoryPort, certificatePdfGeneratorPort);
        mockMvc = MockMvcBuilders.standaloneSetup(new CertificateController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn200WithThePdfFileAndHeaders() throws Exception {
        mockMvc.perform(get(URL, ISSUED_CODE))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename=\"certificado-marketing-digital-para-hospitalidade.pdf\"")))
                .andExpect(content().bytes(GENERATED_PDF));
    }

    @Test
    void shouldReturn404WhenVerificationCodeDoesNotExist() throws Exception {
        mockMvc.perform(get(URL, "DOES-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Certificate not found: DOES-NOT-EXIST"));
    }
}
