package com.escapa.backend.adapters.controller;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import com.escapa.backend.domain.course.EnrollmentStatus;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserCourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseId;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.entity.AdminEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentEnrollmentControllerTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserCourseJpaRepository userCourseJpaRepository;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserEntity student;
    private AdminEntity instructor;

    @BeforeEach
    void setUp() {
        userCourseJpaRepository.deleteAll();
        courseJpaRepository.deleteAll();
        userJpaRepository.deleteAll();

        student = new UserEntity(UUID.randomUUID(), "Student Test", "student@test.com", "hash", "STUDENT", LocalDateTime.now());
        userJpaRepository.save(student);
        instructor = new AdminEntity(UUID.randomUUID(), "Instructor Test", "instructor@test.com", "hash", "ADMIN", LocalDateTime.now(), "TI");
        userJpaRepository.save(instructor);
    }

    @AfterEach
    void tearDown() {
        userCourseJpaRepository.deleteAll();
        courseJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/student/enrollments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldListEnrollmentsWithCorrectStatus() throws Exception {
        CourseEntity course1 = createCourse("Course 1");
        createUserCourse(student, course1, LocalDate.now().minusDays(10), null, 100, null);
        CourseEntity course2 = createCourse("Course 2");
        createUserCourse(student, course2, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), 50, null);
        CourseEntity course3 = createCourse("Course 3");
        createUserCourse(student, course3, LocalDate.now().plusDays(5), null, 0, null);
        CourseEntity course4 = createCourse("Course 4");
        createUserCourse(student, course4, LocalDate.now().minusDays(1), null, 10, null);
        mockMvc.perform(get("/api/v1/student/enrollments")
                        .header("X-User-Id", student.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(4)))
                .andExpect(jsonPath("$.data.content[?(@.enrollmentStatus == 'COMPLETED')]", hasSize(1)))
                .andExpect(jsonPath("$.data.content[?(@.enrollmentStatus == 'EXPIRED')]", hasSize(1)))
                .andExpect(jsonPath("$.data.content[?(@.enrollmentStatus == 'PENDING')]", hasSize(1)))
                .andExpect(jsonPath("$.data.content[?(@.enrollmentStatus == 'IN_PROGRESS')]", hasSize(1)));
    }

    @Test
    void shouldFilterEnrollmentsByStatusAndTitle() throws Exception {
        CourseEntity courseA = createCourse("Java Basics");
        createUserCourse(student, courseA, LocalDate.now().minusDays(1), null, 10, null);

        CourseEntity courseB = createCourse("Advanced Java");
        createUserCourse(student, courseB, LocalDate.now().minusDays(10), null, 100, LocalDate.now());
        mockMvc.perform(get("/api/v1/student/enrollments?query=java")
                        .header("X-User-Id", student.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(2)));
        mockMvc.perform(get("/api/v1/student/enrollments?query=java&status=COMPLETED")
                        .header("X-User-Id", student.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("Advanced Java")));
    }

    private CourseEntity createCourse(String title) {
        CourseEntity course = new CourseEntity();
        course.setTitle(title);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setInstructor(instructor);
        return courseJpaRepository.save(course);
    }

    private UserCourseEntity createUserCourse(UserEntity user, CourseEntity course, LocalDate dtInicio, LocalDate dtExpiracao, Integer progress, LocalDate conclusionDate) {
        UserCourseEntity uc = new UserCourseEntity();
        uc.setId(new UserCourseId(user.getId(), course.getId()));
        uc.setUser(user);
        uc.setCourse(course);
        uc.setDtInicio(dtInicio);
        uc.setDtExpiracao(dtExpiracao);
        uc.setProgress(progress);
        uc.setConclusionDate(conclusionDate);
        uc.setCertificateIssued(false);
        return userCourseJpaRepository.save(uc);
    }
}

