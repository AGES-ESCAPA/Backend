package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.GetAdminCourseMetricsUseCase;
import com.escapa.backend.application.usecase.InMemoryEnrollmentRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCourseMetricsControllerTest {

    private static final String USER_HEADER = "Authorization";
    private static final String METRICS_URL = "/api/v1/admin/metrics/courses";

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryEnrollmentRepositoryPort enrollmentRepository = new InMemoryEnrollmentRepositoryPort();

    private String adminId;
    private String studentId;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(new User("Admin Escapa", "admin@escapa.com", "hash", "ADMIN")).getId().toString();
        studentId = userRepository.save(new User("Aluno Escapa", "aluno@escapa.com", "hash", "STUDENT")).getId().toString();

        final AdminCourseMetricsController controller = new AdminCourseMetricsController(
                new GetAdminCourseMetricsUseCase(enrollmentRepository),
                new AdminRequestGuard(new AuthorizeAdminUseCase(userRepository))
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate(adminId, "ADMIN");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturn401WhenRequestHasNoAuthentication() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get(METRICS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing or invalid access token"));
    }

    @Test
    void shouldReturn403WhenAuthenticatedUserIsNotAdmin() throws Exception {
        authenticate(studentId, "STUDENT");

        mockMvc.perform(get(METRICS_URL).header(USER_HEADER, "Bearer student-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: user is not ADMIN"));
    }

    @Test
    void shouldReturn200WithEmptyContractWhenNoEligibleEnrollmentsExist() throws Exception {
        mockMvc.perform(get(METRICS_URL).header(USER_HEADER, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mostAccessedCourse").value(nullValue()))
                .andExpect(jsonPath("$.data.topCourses").isArray())
                .andExpect(jsonPath("$.data.topCourses.length()").value(0))
                .andExpect(jsonPath("$.data.categoriesCount").value(0))
                .andExpect(jsonPath("$.data.enrollmentsByCategory").isArray())
                .andExpect(jsonPath("$.data.enrollmentsByCategory.length()").value(0))
                .andExpect(jsonPath("$.message").value("Operation completed successfully"));
    }

    @Test
    void shouldReturn200WithCourseMetricsWrappedInApiResponse() throws Exception {
        final UUID courseA = UUID.fromString("e0000000-0000-4000-e000-000000000001");
        final UUID courseB = UUID.fromString("e0000000-0000-4000-e000-000000000002");

        enrollmentRepository.registerCourse(courseA, "Turismo de Aventura", "Hospitalidade", CourseStatus.PUBLISHED);
        enrollmentRepository.registerCourse(courseB, "IA no Turismo", "Inteligência Artificial", CourseStatus.PUBLISHED);

        enrollmentRepository.enroll(UUID.randomUUID(), courseA, new Enrollment(LocalDate.now(), null));
        enrollmentRepository.enroll(UUID.randomUUID(), courseA, new Enrollment(LocalDate.now(), null));
        enrollmentRepository.enroll(UUID.randomUUID(), courseB, new Enrollment(LocalDate.now(), null));

        mockMvc.perform(get(METRICS_URL).header(USER_HEADER, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mostAccessedCourse.courseId").value(courseA.toString()))
                .andExpect(jsonPath("$.data.mostAccessedCourse.title").value("Turismo de Aventura"))
                .andExpect(jsonPath("$.data.mostAccessedCourse.enrollments").value(2))
                .andExpect(jsonPath("$.data.topCourses.length()").value(2))
                .andExpect(jsonPath("$.data.topCourses[0].courseId").value(courseA.toString()))
                .andExpect(jsonPath("$.data.topCourses[0].title").value("Turismo de Aventura"))
                .andExpect(jsonPath("$.data.topCourses[0].enrollments").value(2))
                .andExpect(jsonPath("$.data.topCourses[1].courseId").value(courseB.toString()))
                .andExpect(jsonPath("$.data.topCourses[1].title").value("IA no Turismo"))
                .andExpect(jsonPath("$.data.topCourses[1].enrollments").value(1))
                .andExpect(jsonPath("$.data.categoriesCount").value(2))
                .andExpect(jsonPath("$.data.enrollmentsByCategory[0].category").value("Hospitalidade"))
                .andExpect(jsonPath("$.data.enrollmentsByCategory[0].percentage").value(66.67))
                .andExpect(jsonPath("$.data.enrollmentsByCategory[1].category").value("Inteligência Artificial"))
                .andExpect(jsonPath("$.data.enrollmentsByCategory[1].percentage").value(33.33))
                .andExpect(jsonPath("$.message").value("Operation completed successfully"));
    }

    private static void authenticate(String userId, String role) {
        final var user = new AuthenticatedUser(UUID.fromString(userId), "test@escapa.com", role, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }
}

