package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.usecase.GetCourseDetailsUseCase;
import com.escapa.backend.application.usecase.InMemoryCourseRepositoryPort;
import com.escapa.backend.application.usecase.ListPublishedCourseFiltersUseCase;
import com.escapa.backend.application.usecase.ListPublishedCoursesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Formato HTTP da vitrine pública: lista e filtros sem envelope, detalhes dentro de {@code ApiResponse}. */
class PublicCourseControllerTest {

    private static final String COURSES_URL = "/api/v1/public/courses";

    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final PublicCourseController controller = new PublicCourseController(
                new ListPublishedCoursesUseCase(courseRepository),
                new ListPublishedCourseFiltersUseCase(courseRepository),
                new GetCourseDetailsUseCase(courseRepository)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnThePageWithoutTheApiResponseEnvelope() throws Exception {
        courseRepository.addCourse(summary("Turismo Sustentável", "Turismo", "Iniciante"));
        courseRepository.addCourse(summary("Gestão Hoteleira", "Hospitalidade", "Avançado"));

        mockMvc.perform(get(COURSES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].instructor").value("Instrutora"));
    }

    @Test
    void shouldFilterAndPaginate() throws Exception {
        courseRepository.addCourse(summary("Turismo Sustentável", "Turismo", "Iniciante"));
        courseRepository.addCourse(summary("Turismo Rural", "Turismo", "Iniciante"));
        courseRepository.addCourse(summary("Gestão Hoteleira", "Hospitalidade", "Avançado"));

        mockMvc.perform(get(COURSES_URL).param("category", "Turismo").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldListFiltersWithoutTheEnvelope() throws Exception {
        courseRepository.addCourse(summary("Turismo Sustentável", "Turismo", "Iniciante"));
        courseRepository.addCourse(summary("Gestão Hoteleira", "Hospitalidade", "Avançado"));

        mockMvc.perform(get(COURSES_URL + "/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.categories.length()").value(2))
                .andExpect(jsonPath("$.levels.length()").value(2));
    }

    @Test
    void shouldReturnCourseDetailsInsideTheEnvelope() throws Exception {
        final UUID courseId = UUID.randomUUID();
        courseRepository.saveDetails(details(courseId));

        mockMvc.perform(get(COURSES_URL + "/{id}", courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(courseId.toString()))
                .andExpect(jsonPath("$.data.title").value("IA Aplicada ao Turismo"))
                .andExpect(jsonPath("$.data.instructor").doesNotExist())
                .andExpect(jsonPath("$.data.modules.length()").value(0));
    }

    @Test
    void shouldReturn404ForAnUnknownCourse() throws Exception {
        mockMvc.perform(get(COURSES_URL + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForAMalformedCourseId() throws Exception {
        mockMvc.perform(get(COURSES_URL + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    private static CourseSummary summary(String title, String category, String level) {
        return new CourseSummary(
                UUID.randomUUID(), title, "Resumo", category, level, 10, 5, 99.9,
                "https://cdn.example.com/capa.png", "Instrutora", 4.5, 3);
    }

    private static CourseDetails details(UUID id) {
        return new CourseDetails(
                id, "IA Aplicada ao Turismo", "Resumo", "Descrição", "Tecnologia", "Iniciante", 12, 99.9, 365,
                "https://cdn.example.com/capa.png", null, 4.5, 3, 10, null, List.of("Objetivo"), List.of(), List.of());
    }
}
