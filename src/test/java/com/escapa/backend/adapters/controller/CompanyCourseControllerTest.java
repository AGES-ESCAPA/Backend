package com.escapa.backend.adapters.controller;

import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CompanyCourseControllerTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepositoryPort userRepositoryPort;
    @Autowired
    private CourseJpaRepository courseJpaRepository;
    @Autowired
    private TokenProviderPort tokenProviderPort;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from company_courses");
        jdbcTemplate.update("delete from courses");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldListOnlyTheCoursesOfTheCompanyThatWereNotRevoked() throws Exception {
        final UUID companyId = givenCompany();
        final UUID otherCompanyId = givenCompany();
        final UUID acquired = givenCourse();
        final UUID revoked = givenCourse();
        final UUID fromOtherCompany = givenCourse();
        givenCompanyCourse(companyId, acquired, false);
        givenCompanyCourse(companyId, revoked, true);
        givenCompanyCourse(otherCompanyId, fromOtherCompany, false);

        mockMvc.perform(get("/api/v1/company/courses/acquired")
                        .header("Authorization", authorization(companyId, "COMPANY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", contains(acquired.toString())));
    }

    @Test
    void shouldReturnEmptyListWhenTheCompanyHasNoCourses() throws Exception {
        final UUID companyId = givenCompany();

        mockMvc.perform(get("/api/v1/company/courses/acquired")
                        .header("Authorization", authorization(companyId, "COMPANY")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    void shouldRejectProfilesOtherThanCompany() throws Exception {
        final UUID studentId = givenUser("STUDENT");

        mockMvc.perform(get("/api/v1/company/courses/acquired")
                        .header("Authorization", authorization(studentId, "STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/company/courses/acquired"))
                .andExpect(status().isUnauthorized());
    }

    private UUID givenUser(String role) {
        return userRepositoryPort.save(
                new User("Usuario", "u-" + UUID.randomUUID() + "@email.com", "hash", role)).getId();
    }

    private UUID givenCompany() {
        final UUID companyId = givenUser("COMPANY");
        jdbcTemplate.update(
                "insert into company (id, company_name, cnpj_id, seats_total) values (?, ?, ?, 0)",
                companyId, "Hotel Teste LTDA", UUID.randomUUID().toString().substring(0, 14));
        return companyId;
    }

    private UUID givenCourse() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso " + UUID.randomUUID());
        return courseJpaRepository.save(course).getId();
    }

    private void givenCompanyCourse(UUID companyId, UUID courseId, boolean revoked) {
        jdbcTemplate.update("insert into company_courses (company_id, course_id, data_inicio, revoked_at) "
                + "values (?, ?, current_date, " + (revoked ? "now()" : "null") + ")", companyId, courseId);
    }

    private String authorization(UUID userId, String role) {
        return "Bearer " + tokenProviderPort.generateToken(userId, "u@email.com", role, role);
    }
}
