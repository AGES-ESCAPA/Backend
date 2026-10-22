package com.escapa.backend.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.port.CertificateRepositoryPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.domain.certificate.CertificateTheme;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserCourseJpaRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenSecurityIntegrationTest extends PostgresIntegrationTest {

    private static final String STUDENT_EMAIL = "student.security@escapa.com";
    private static final String ADMIN_EMAIL = "admin.security@escapa.com";
    private static final String CERTIFICATE_CODE = "ESC-SECURITY-2026";

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

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private UUID studentId;
    private UUID adminId;
    private CourseEntity certificateCourse;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        userJpaRepository.save(new UserEntity(
                studentId, "Student Security", STUDENT_EMAIL, "hash", "STUDENT", LocalDateTime.now()));
        userJpaRepository.save(new UserEntity(
                adminId, "Admin Security", ADMIN_EMAIL, "hash", "ADMIN", LocalDateTime.now()));
        certificateCourse = new CourseEntity();
        certificateCourse.setTitle("Curso do Certificado");
        certificateCourse = courseJpaRepository.save(certificateCourse);
        final UserCourseEntity certificateEnrollment = new UserCourseEntity();
        certificateEnrollment.setId(new UserCourseId(studentId, certificateCourse.getId()));
        certificateEnrollment.setUser(userJpaRepository.getReferenceById(studentId));
        certificateEnrollment.setCourse(certificateCourse);
        certificateEnrollment.setCertificateCode(CERTIFICATE_CODE);
        certificateEnrollment.setCertificateIssued(true);
        certificateEnrollment.setConclusionDate(LocalDate.of(2026, 10, 1));
        userCourseJpaRepository.save(certificateEnrollment);
        certificateRepositoryPort.saveCachedPdf(
                CERTIFICATE_CODE, CertificateTheme.DARK, "v2", "%PDF-security".getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        userCourseJpaRepository.deleteById(new UserCourseId(studentId, certificateCourse.getId()));
        courseJpaRepository.deleteById(certificateCourse.getId());
        userJpaRepository.deleteById(studentId);
        userJpaRepository.deleteById(adminId);
    }

    @Test
    void shouldReturnApiErrorWhenProtectedRouteHasNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/student/enrollments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/v1/student/enrollments")));
    }

    @Test
    void shouldRequireTokenForAuthenticatedUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.path", is("/api/v1/auth/me")));
    }

    @Test
    void shouldReturnApiErrorWhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/student/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void shouldReturn401WhenTokenIsExpired() throws Exception {
        mockMvc.perform(get("/api/v1/student/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(expiredStudentToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderDoesNotUseBearerPrefix() throws Exception {
        mockMvc.perform(get("/api/v1/student/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, studentToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    void shouldBlockStudentFromAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void shouldAuthorizeAdminAndUseBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAuthorizeStudentRoutesWithBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/student/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPassAuthenticatedRequestsToEachAdminAndStudentControllerGroup() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses/{courseId}/modules", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/admin/modules/{moduleId}/contents", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/student/courses/{courseId}/curriculum", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnLoggedInUserFromToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(studentId.toString())))
                .andExpect(jsonPath("$.data.name", is("Student Security")))
                .andExpect(jsonPath("$.data.email", is(STUDENT_EMAIL)))
                .andExpect(jsonPath("$.data.profile", is("STUDENT")));
    }

    @Test
    void shouldProtectUserManagementWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isOk());
    }

    @Test
    void shouldProtectUserCreationWithAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldKeepPublicRoutesAccessibleWithoutATokenOrWithAnInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/public/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/certificates/" + CERTIFICATE_CODE))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/register/company")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/certificates/" + CERTIFICATE_CODE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowCertificateDownloadOnlyForItsOwner() throws Exception {
        mockMvc.perform(get("/api/v1/certificates/" + CERTIFICATE_CODE + "/download"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/certificates/" + CERTIFICATE_CODE + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/certificates/" + CERTIFICATE_CODE + "/download")
                        .header(HttpHeaders.AUTHORIZATION, bearer(studentToken())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    void shouldAllowAuthorizationHeaderFromConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("Authorization")));
    }

    private String studentToken() {
        return tokenProviderPort.generateToken(studentId, STUDENT_EMAIL, "STUDENT", "STUDENT");
    }

    private String adminToken() {
        return tokenProviderPort.generateToken(adminId, ADMIN_EMAIL, "ADMIN", "ADMIN");
    }

    private String expiredStudentToken() {
        return new JwtTokenProviderAdapter(jwtSecret, -1L)
                .generateToken(studentId, STUDENT_EMAIL, "STUDENT", "STUDENT");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
