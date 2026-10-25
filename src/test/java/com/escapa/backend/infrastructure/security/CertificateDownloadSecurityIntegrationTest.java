package com.escapa.backend.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.port.CertificatePdfGeneratorPort;
import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.domain.certificate.CertificateTheme;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import com.escapa.backend.infrastructure.persistence.UserCourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Download do certificado com a cadeia real do Spring Security e o filtro JWT:
 * so o aluno dono recebe o PDF. O gerador e mockado para nao subir o Chromium.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "certificate.pdf.warm-up=false")
class CertificateDownloadSecurityIntegrationTest extends PostgresIntegrationTest {

    private static final String DOWNLOAD_URL = "/api/v1/certificates/{verificationCode}/download";
    private static final String DETAILS_URL = "/api/v1/certificates/{verificationCode}";
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final String TEMPLATE_VERSION = "v-security-test";
    private static final byte[] GENERATED_PDF = "%PDF-generated".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CACHED_PDF = "%PDF-cached".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserCourseJpaRepository userCourseJpaRepository;

    @Autowired
    private CertificateRepositoryPort certificateRepositoryPort;

    @MockitoBean
    private CertificatePdfGeneratorPort certificatePdfGeneratorPort;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private final List<UserCourseId> enrollments = new ArrayList<>();
    private final List<UUID> courses = new ArrayList<>();
    private final List<UUID> users = new ArrayList<>();

    private UUID ownerId;
    private UUID otherStudentId;
    private String issuedCode;
    private String cachedCode;
    private String unconcludedCode;

    @BeforeEach
    void setUp() {
        final String suffix = UUID.randomUUID().toString().substring(0, 8);
        ownerId = givenStudent("owner." + suffix + "@escapa.com");
        otherStudentId = givenStudent("other." + suffix + "@escapa.com");
        issuedCode = "ESC-DL-" + suffix + "-1";
        cachedCode = "ESC-DL-" + suffix + "-2";
        unconcludedCode = "ESC-DL-" + suffix + "-3";
        givenCertificate(ownerId, issuedCode, LocalDate.of(2026, 10, 1));
        givenCertificate(ownerId, cachedCode, LocalDate.of(2026, 10, 2));
        givenCertificate(ownerId, unconcludedCode, null);
        certificateRepositoryPort.saveCachedPdf(cachedCode, CertificateTheme.DARK, TEMPLATE_VERSION, CACHED_PDF);

        when(certificatePdfGeneratorPort.templateVersion()).thenReturn(TEMPLATE_VERSION);
        when(certificatePdfGeneratorPort.generate(any(), any())).thenReturn(GENERATED_PDF);
    }

    @AfterEach
    void tearDown() {
        enrollments.forEach(userCourseJpaRepository::deleteById);
        courses.forEach(courseJpaRepository::deleteById);
        users.forEach(userJpaRepository::deleteById);
    }

    @Test
    void shouldReturnThePdfToTheOwner() throws Exception {
        mockMvc.perform(download(issuedCode).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(ownerId))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(content().bytes(GENERATED_PDF));
    }

    @Test
    void shouldServeTheCachedPdfToTheOwner() throws Exception {
        mockMvc.perform(download(cachedCode).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(ownerId))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(CACHED_PDF));
    }

    @Test
    void shouldReturn401WithoutAToken() throws Exception {
        mockMvc.perform(download(issuedCode))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void shouldReturn401ForAMalformedToken() throws Exception {
        mockMvc.perform(download(issuedCode).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));

        mockMvc.perform(download(issuedCode).header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401ForAnExpiredToken() throws Exception {
        final String expired = new JwtTokenProviderAdapter(jwtSecret, -1L)
                .generateToken(ownerId, "owner@escapa.com", "STUDENT", "STUDENT");

        mockMvc.perform(download(issuedCode).header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void shouldReturn403ForAnotherAuthenticatedStudent() throws Exception {
        mockMvc.perform(download(issuedCode).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherStudentId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        verify(certificatePdfGeneratorPort, never()).generate(any(), any());
    }

    @Test
    void shouldReturn403ForAnotherStudentEvenWhenThePdfIsCached() throws Exception {
        mockMvc.perform(download(cachedCode).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherStudentId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void shouldReturn404ForAnUnknownCode() throws Exception {
        mockMvc.perform(download("ESC-DOES-NOT-EXIST").header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(ownerId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void shouldReturn404WhenTheCourseIsNotConcludedEvenForTheOwner() throws Exception {
        mockMvc.perform(download(unconcludedCode).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(ownerId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCheckAuthenticationBeforeTheCodeExists() throws Exception {
        mockMvc.perform(download("ESC-DOES-NOT-EXIST"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCheckTheCodeExistsBeforeTheOwnership() throws Exception {
        mockMvc.perform(download("ESC-DOES-NOT-EXIST")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherStudentId))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateTheThemeAfterAuthenticationAndBeforeTheCodeAndOwnership() throws Exception {
        mockMvc.perform(download("ESC-DOES-NOT-EXIST").param("theme", "blue"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(download("ESC-DOES-NOT-EXIST").param("theme", "blue")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherStudentId))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(download(issuedCode).param("theme", "blue")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherStudentId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAnswerThePreflightWithoutAuthentication() throws Exception {
        mockMvc.perform(options(DOWNLOAD_URL, issuedCode)
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")));
    }

    @Test
    void shouldExposeContentDispositionToTheFrontendOrigin() throws Exception {
        mockMvc.perform(download(issuedCode)
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(ownerId))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString(HttpHeaders.CONTENT_DISPOSITION)));
    }

    @Test
    void shouldKeepTheCertificateScreenPublic() throws Exception {
        mockMvc.perform(get(DETAILS_URL, issuedCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwner", is(false)));

        mockMvc.perform(get(DETAILS_URL, issuedCode).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwner", is(false)));
    }

    @Test
    void shouldUseTheSameOwnerRuleOnTheScreenAndOnTheDownload() throws Exception {
        mockMvc.perform(get(DETAILS_URL, issuedCode).header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(ownerId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwner", is(true)));

        mockMvc.perform(get(DETAILS_URL, issuedCode)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(otherStudentId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOwner", is(false)));
    }

    private UUID givenStudent(String email) {
        final UUID id = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(id, "Student " + email, email, "hash", "STUDENT", LocalDateTime.now()));
        users.add(id);
        return id;
    }

    private void givenCertificate(UUID studentId, String code, LocalDate conclusionDate) {
        CourseEntity course = new CourseEntity();
        course.setTitle("Curso " + code);
        course = courseJpaRepository.save(course);
        courses.add(course.getId());

        final UserCourseEntity enrollment = new UserCourseEntity();
        enrollment.setId(new UserCourseId(studentId, course.getId()));
        enrollment.setUser(userJpaRepository.getReferenceById(studentId));
        enrollment.setCourse(course);
        enrollment.setCertificateCode(code);
        enrollment.setCertificateIssued(true);
        enrollment.setConclusionDate(conclusionDate);
        userCourseJpaRepository.save(enrollment);
        enrollments.add(enrollment.getId());
    }

    private String tokenFor(UUID userId) {
        final UserEntity user = userJpaRepository.findById(userId).orElseThrow();
        return tokenProviderPort.generateToken(userId, user.getEmail(), "STUDENT", "STUDENT");
    }

    private static MockHttpServletRequestBuilder download(String code) {
        return get(DOWNLOAD_URL, code);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
