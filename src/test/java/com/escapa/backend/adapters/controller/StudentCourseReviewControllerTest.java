package com.escapa.backend.adapters.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.usecase.InMemoryCourseReviewRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryEnrollmentRepositoryPort;
import com.escapa.backend.application.usecase.RateCourseUseCase;

class StudentCourseReviewControllerTest {

    private static final String REVIEW_URL = "/api/v1/student/courses/{courseId}/review";

    private final InMemoryEnrollmentRepositoryPort enrollmentRepository = new InMemoryEnrollmentRepositoryPort();
    private final InMemoryCourseReviewRepositoryPort reviewRepository = new InMemoryCourseReviewRepositoryPort();
    private final UUID studentId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final StudentCourseReviewController controller = new StudentCourseReviewController(
                new RateCourseUseCase(enrollmentRepository, reviewRepository, Clock.systemUTC()));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        final var user = new AuthenticatedUser(studentId, "aluno@escapa.com", "STUDENT", "STUDENT");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_STUDENT")));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private void givenCompletedEnrollment() {
        enrollmentRepository.enroll(studentId, courseId, new Enrollment(LocalDate.now().minusDays(30), null));
        enrollmentRepository.complete(studentId, courseId);
    }

    private static String body(Object rating) {
        return "{\"rating\": " + rating + "}";
    }

    @Test
    void shouldRateCompletedCourse() throws Exception {
        givenCompletedEnrollment();

        mockMvc.perform(put(REVIEW_URL, courseId).contentType(MediaType.APPLICATION_JSON).content(body(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.data.rating").value(5));

        assertEquals(5, reviewRepository.ratingOf(studentId, courseId));
    }

    @Test
    void shouldRejectRequestsWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(put(REVIEW_URL, courseId).contentType(MediaType.APPLICATION_JSON).content(body(5)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidRatingCourseNotCompleted() throws Exception {
        enrollmentRepository.enroll(studentId, courseId, new Enrollment(LocalDate.now().minusDays(30), null));

        mockMvc.perform(put(REVIEW_URL, courseId).contentType(MediaType.APPLICATION_JSON).content(body(4)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only students who completed the course can rate it"));

        assertNull(reviewRepository.ratingOf(studentId, courseId));
    }

    @Test
    void shouldRejectRatingOutOfRange() throws Exception {
        givenCompletedEnrollment();

        mockMvc.perform(put(REVIEW_URL, courseId).contentType(MediaType.APPLICATION_JSON).content(body(6)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(REVIEW_URL, courseId).contentType(MediaType.APPLICATION_JSON).content(body(0)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(REVIEW_URL, courseId).contentType(MediaType.APPLICATION_JSON).content(body(null)))
                .andExpect(status().isBadRequest());

        assertNull(reviewRepository.ratingOf(studentId, courseId));
    }
}
