package com.escapa.backend.course.catalog.controller;

import com.escapa.backend.common.WebIntegrationTest;
import com.escapa.backend.course.management.repository.CourseRepository;
import com.escapa.backend.course.shared.entity.CourseEntity;
import com.escapa.backend.course.shared.entity.CourseStatus;
import com.escapa.backend.user.entity.AdminEntity;
import com.escapa.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP da US-01: rota pública, nomes exatos dos campos do JSON e erros de parâmetro.
 * Cada teste filtra por um título único, então não depende de limpeza entre classes.
 */
class CourseCatalogControllerTest extends WebIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    private String uniqueTitle;

    @BeforeEach
    void seedOnePublishedCourse() {
        uniqueTitle = "Vitrine " + UUID.randomUUID();
        final AdminEntity instructor = userRepository.save(new AdminEntity(UUID.randomUUID(), "Dra. Mariana",
                "vitrine." + UUID.randomUUID().toString().substring(0, 8) + "@escapa.com",
                "hash", "ADMIN", LocalDateTime.now(), "Turismo"));

        final CourseEntity course = new CourseEntity();
        course.setTitle(uniqueTitle);
        course.setShortDescription("Domine as ferramentas de IA");
        course.setCategory("Inteligência Artificial");
        course.setLevel("Iniciante");
        course.setStatus(CourseStatus.PUBLISHED);
        course.setInstructor(instructor);
        course.setCreatedBy(instructor);
        course.setCreatedAt(LocalDateTime.now());
        course.setDurationTime(12);
        course.setLessonsCount(32);
        course.setPrice(97.0);
        course.setThumbnailUrl("https://cdn.escapa.com.br/courses/101/thumb.jpg");
        course.setRatingAverage(4.8);
        course.setReviewsCount(56);
        courseRepository.save(course);
    }

    @Test
    void shouldAnswerWithoutAuthenticationUsingTheUsJsonContract() throws Exception {
        mockMvc.perform(get("/api/v1/public/courses")
                        .param("title", uniqueTitle)
                        .param("category", "inteligência artificial")
                        .param("level", "INICIANTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].title").value(uniqueTitle))
                .andExpect(jsonPath("$.content[0].shortDescription").value("Domine as ferramentas de IA"))
                .andExpect(jsonPath("$.content[0].category").value("Inteligência Artificial"))
                .andExpect(jsonPath("$.content[0].level").value("Iniciante"))
                .andExpect(jsonPath("$.content[0].durationTime").value(12))
                .andExpect(jsonPath("$.content[0].lessonsCount").value(32))
                .andExpect(jsonPath("$.content[0].price").value(97.0))
                .andExpect(jsonPath("$.content[0].thumbnailUrl").value("https://cdn.escapa.com.br/courses/101/thumb.jpg"))
                .andExpect(jsonPath("$.content[0].instructor").value("Dra. Mariana"))
                .andExpect(jsonPath("$.content[0].ratingAverage").value(4.8))
                .andExpect(jsonPath("$.content[0].reviewsCount").value(56))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void shouldAnswerEmptyPageWithStatus200WhenNothingMatches() throws Exception {
        mockMvc.perform(get("/api/v1/public/courses").param("category", "categoria-inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldAnswer400WhenPageIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/v1/public/courses").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'page'"));
    }

    @Test
    void shouldHonorPageSizeAndReportTotals() throws Exception {
        mockMvc.perform(get("/api/v1/public/courses").param("title", uniqueTitle).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}
