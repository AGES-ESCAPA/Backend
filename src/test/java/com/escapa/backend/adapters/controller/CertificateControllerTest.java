package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.CertificateDetails;
import com.escapa.backend.application.model.FormattedCertificate;
import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.application.usecase.DownloadCertificateUseCase;
import com.escapa.backend.application.usecase.GetCertificateDetailsUseCase;
import com.escapa.backend.domain.certificate.CertificateGenerationException;
import com.escapa.backend.domain.certificate.CertificateGenerationUnavailableException;
import com.escapa.backend.domain.certificate.CertificateTheme;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

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
    private static final UUID STUDENT_ID = UUID.fromString("a0000000-0000-4000-a000-000000000011");

    private MockMvc mockMvc;
    // Erro que o generator fake lanca, quando definido. Sem erro, ele devolve
    // "%PDF-<tema>", para o teste conferir qual tema chegou ate ele.
    private Supplier<RuntimeException> generationFailure = () -> null;

    @BeforeEach
    void setUp() {
        final CertificateDetails.Course course = new CertificateDetails.Course(
                COURSE_ID, "Marketing Digital para Hospitalidade",
                "Estrategias de marketing digital para hoteis", "Marketing", "Intermediario",
                "https://cdn.escapa.com.br/courses/102/thumb.jpg", 960, 44, 5.0, 183,
                "Thamiris Magalhaes", 117.0);
        final CertificateDetails.Student student = new CertificateDetails.Student(
                STUDENT_ID, "Jorge Amado", "https://cdn.escapa.com.br/users/42/avatar.jpg", true);
        final Map<String, CertificateDetails> detailsByCode = Map.of(
                ISSUED_CODE, new CertificateDetails(LocalDate.of(2026, 8, 21), 960, ISSUED_CODE, student, course),
                UNCONCLUDED_CODE, new CertificateDetails(null, 960, UNCONCLUDED_CODE, student, course));

        final CertificateRepositoryPort certificateRepositoryPort = new CertificateRepositoryPort() {
            @Override
            public Optional<CertificateDetails> findDetailsByVerificationCode(String verificationCode) {
                return Optional.ofNullable(detailsByCode.get(verificationCode));
            }

            @Override
            public Optional<byte[]> findCachedPdf(String code, CertificateTheme theme, String version) {
                // Sem cache nesse teste: todo download passa pelo generator fake.
                return Optional.empty();
            }

            @Override
            public void saveCachedPdf(String code, CertificateTheme theme, String version, byte[] pdf) {
                // Sem cache nesse teste.
            }
        };
        final CertificatePdfGeneratorPort certificatePdfGeneratorPort = new CertificatePdfGeneratorPort() {
            @Override
            public String templateVersion() {
                return "v2";
            }

            @Override
            public byte[] generate(FormattedCertificate certificate, CertificateTheme theme) {
                final RuntimeException failure = generationFailure.get();
                if (failure != null) {
                    throw failure;
                }
                return pdfFor(theme);
            }
        };

        final GetCertificateDetailsUseCase detailsUseCase = new GetCertificateDetailsUseCase(certificateRepositoryPort);
        final DownloadCertificateUseCase useCase =
                new DownloadCertificateUseCase(detailsUseCase, certificateRepositoryPort, certificatePdfGeneratorPort);
        mockMvc = MockMvcBuilders.standaloneSetup(new CertificateController(detailsUseCase, useCase, "dark"))
                .setControllerAdvice(new GlobalExceptionHandler())
                // Mesmo ObjectMapper do Spring Boot: datas em ISO-8601 ("2026-08-21"), nao em array.
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
        authenticateAs(STUDENT_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static byte[] pdfFor(CertificateTheme theme) {
        return ("%PDF-" + theme).getBytes(StandardCharsets.UTF_8);
    }

    private static void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, "student@escapa.com", "STUDENT", "STUDENT"), null));
    }

    @Test
    void shouldReturn200WithThePdfFileAndHeaders() throws Exception {
        mockMvc.perform(get(URL, ISSUED_CODE).param("theme", "dark"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename=\"certificado-marketing-digital-para-hospitalidade.pdf\"")))
                .andExpect(content().bytes(pdfFor(CertificateTheme.DARK)));
    }

    @Test
    void shouldUseTheDarkThemeWhenTheParameterIsMissing() throws Exception {
        mockMvc.perform(get(URL, ISSUED_CODE))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfFor(CertificateTheme.DARK)));
    }

    @Test
    void shouldAcceptTheThemeIgnoringCase() throws Exception {
        mockMvc.perform(get(URL, ISSUED_CODE).param("theme", "LIGHT"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfFor(CertificateTheme.LIGHT)));
    }

    @Test
    void shouldReturn400ForAnInvalidTheme() throws Exception {
        mockMvc.perform(get(URL, ISSUED_CODE).param("theme", "blue"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid theme 'blue': use light or dark"));
    }

    @Test
    void shouldReturn404WhenVerificationCodeDoesNotExist() throws Exception {
        mockMvc.perform(get(URL, "DOES-NOT-EXIST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Certificate not found"));
    }

    @Test
    void shouldReturn403WhenTheAuthenticatedUserDoesNotOwnTheCertificate() throws Exception {
        authenticateAs(UUID.randomUUID());

        mockMvc.perform(get(URL, ISSUED_CODE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: user is not the certificate owner"));
    }

    @Test
    void shouldReturn404WhenTheCourseIsNotConcluded() throws Exception {
        mockMvc.perform(get(URL, UNCONCLUDED_CODE).param("theme", "dark"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn503WhenTheGeneratorIsBusy() throws Exception {
        generationFailure = () -> new CertificateGenerationUnavailableException("Certificate generation is busy");

        mockMvc.perform(get(URL, ISSUED_CODE))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void shouldReturnAGeneric500WhenTheGenerationFails() throws Exception {
        generationFailure = () -> new CertificateGenerationException(
                "Failed to generate the certificate PDF", new IllegalStateException("Chromium crashed"));

        mockMvc.perform(get(URL, ISSUED_CODE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Could not generate the certificate PDF"));
    }

    @Test
    void shouldMarkTheScreenAsOwnedOnlyForTheAuthenticatedOwner() throws Exception {
        mockMvc.perform(get(DETAILS_URL, ISSUED_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwner").value(true));

        authenticateAs(UUID.randomUUID());
        mockMvc.perform(get(DETAILS_URL, ISSUED_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwner").value(false));
    }

    @Test
    void shouldReturn200WithTheCertificateScreenData() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get(DETAILS_URL, ISSUED_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.certificate.conclusionDate").value("2026-08-21"))
                .andExpect(jsonPath("$.data.certificate.workload").value(960))
                .andExpect(jsonPath("$.data.certificate.verificationCode").value(ISSUED_CODE))
                .andExpect(jsonPath("$.data.certificate.formattedConclusionDate").value("21 de agosto de 2026"))
                .andExpect(jsonPath("$.data.certificate.formattedWorkload").value("16 horas"))
                .andExpect(jsonPath("$.data.certificate.validationUrl")
                        .value("escapa.com.br/certificados/" + ISSUED_CODE))
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
                .andExpect(jsonPath("$.message").value("Certificate not found"));
    }

    @Test
    void shouldReturn404ForTheScreenWhenCourseIsNotConcluded() throws Exception {
        mockMvc.perform(get(DETAILS_URL, UNCONCLUDED_CODE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
