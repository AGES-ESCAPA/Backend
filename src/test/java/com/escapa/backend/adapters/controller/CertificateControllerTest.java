package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.CertificateRecord;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.application.usecase.DownloadCertificateUseCase;
import com.escapa.backend.application.usecase.GetCertificateDetailsUseCase;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP das US-18 e US-19: os casos de uso reais rodam sobre portas em memoria. */
class CertificateControllerTest {

    private static final String URL = "/api/v1/certificates/{verificationCode}/download";
    private static final String DETAILS_URL = "/api/v1/certificates/{verificationCode}";
    private static final String ISSUED_CODE = "ESC-2026-MKT-0001";
    private static final String UNCONCLUDED_CODE = "ESC-2026-MKT-0002";
    private static final UUID COURSE_ID = UUID.fromString("b2c3d4e5-1111-2222-3333-444455556666");
    private static final byte[] GENERATED_PDF = "%PDF-1.4 fake content".getBytes(StandardCharsets.UTF_8);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final CertificateRecord certificate = new CertificateRecord(
                UUID.randomUUID(), UUID.randomUUID(), "Jorge Amado", "Marketing Digital para Hospitalidade",
                960, LocalDate.of(2026, 8, 21), ISSUED_CODE, null);
        final CertificateDetails.Course course = new CertificateDetails.Course(
                COURSE_ID, "Marketing Digital para Hospitalidade",
                "Estrategias de marketing digital para hoteis", "Marketing", "Intermediario",
                "https://cdn.escapa.com.br/courses/102/thumb.jpg", 960, 44, 5.0, 183,
                "Thamiris Magalhaes", 117.0);
        final CertificateDetails.Student student = new CertificateDetails.Student(
                "Jorge Amado", "https://cdn.escapa.com.br/users/42/avatar.jpg", true);
        final Map<String, CertificateDetails> detailsByCode = Map.of(
                ISSUED_CODE, new CertificateDetails(LocalDate.of(2026, 8, 21), 960, ISSUED_CODE, student, course),
                UNCONCLUDED_CODE, new CertificateDetails(null, 960, UNCONCLUDED_CODE, student, course));

        final CertificateRepositoryPort certificateRepositoryPort =
                new CertificateRepositoryPort() {
                    @Override
                    public Optional<CertificateRecord> findByVerificationCode(String verificationCode) {
                        return Optional.of(certificate).filter(c -> c.verificationCode().equals(verificationCode));
                    }

                    @Override
                    public Optional<CertificateDetails> findDetailsByVerificationCode(String verificationCode) {
                        return Optional.ofNullable(detailsByCode.get(verificationCode));
                    }

                    @Override
                    public void saveCachedPdf(UUID userId, UUID courseId, byte[] pdf) {
                        // Sem cache real nesse teste: o generator fake sempre devolve o mesmo PDF.
                    }
                };
        final CertificatePdfGeneratorPort certificatePdfGeneratorPort = c -> GENERATED_PDF;

        final DownloadCertificateUseCase useCase =
                new DownloadCertificateUseCase(certificateRepositoryPort, certificatePdfGeneratorPort);
        final GetCertificateDetailsUseCase detailsUseCase = new GetCertificateDetailsUseCase(certificateRepositoryPort);
        mockMvc = MockMvcBuilders.standaloneSetup(new CertificateController(detailsUseCase, useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                // Mesmo ObjectMapper do Spring Boot: datas em ISO-8601 ("2026-08-21"), nao em array.
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
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

    @Test
    void shouldReturn200WithTheCertificateScreenData() throws Exception {
        mockMvc.perform(get(DETAILS_URL, ISSUED_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificate.conclusionDate").value("2026-08-21"))
                .andExpect(jsonPath("$.data.certificate.workload").value(960))
                .andExpect(jsonPath("$.data.certificate.verificationCode").value(ISSUED_CODE))
                .andExpect(jsonPath("$.data.student.name").value("Jorge Amado"))
                .andExpect(jsonPath("$.data.student.avatarUrl").value("https://cdn.escapa.com.br/users/42/avatar.jpg"))
                .andExpect(jsonPath("$.data.student.isVerified").value(true))
                .andExpect(jsonPath("$.data.student.verified").doesNotExist())
                .andExpect(jsonPath("$.data.course.id").value(COURSE_ID.toString()))
                .andExpect(jsonPath("$.data.course.title").value("Marketing Digital para Hospitalidade"))
                .andExpect(jsonPath("$.data.course.category").value("Marketing"))
                .andExpect(jsonPath("$.data.course.level").value("Intermediario"))
                .andExpect(jsonPath("$.data.course.thumbnailUrl").value("https://cdn.escapa.com.br/courses/102/thumb.jpg"))
                .andExpect(jsonPath("$.data.course.durationTime").value(960))
                .andExpect(jsonPath("$.data.course.lessonsCount").value(44))
                .andExpect(jsonPath("$.data.course.rating").value(5.0))
                .andExpect(jsonPath("$.data.course.reviewsCount").value(183))
                .andExpect(jsonPath("$.data.course.instructor").value("Thamiris Magalhaes"))
                .andExpect(jsonPath("$.data.course.price").value(117.0))
                .andExpect(jsonPath("$.data.isOwner").value(false))
                .andExpect(jsonPath("$.data.owner").doesNotExist());
    }

    @Test
    void shouldReturn404ForTheScreenWhenVerificationCodeDoesNotExist() throws Exception {
        mockMvc.perform(get(DETAILS_URL, "DOES-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Certificate not found: DOES-NOT-EXIST"));
    }

    @Test
    void shouldReturn404ForTheScreenWhenCourseIsNotConcluded() throws Exception {
        mockMvc.perform(get(DETAILS_URL, UNCONCLUDED_CODE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
