package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.CreateContentUseCase;
import com.escapa.backend.application.usecase.DeleteContentUseCase;
import com.escapa.backend.application.usecase.GetContentUseCase;
import com.escapa.backend.application.usecase.InMemoryContentRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryModuleRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.ListModuleContentsUseCase;
import com.escapa.backend.application.usecase.ReorderContentsUseCase;
import com.escapa.backend.application.usecase.UpdateContentUseCase;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP e controle de acesso ADMIN das aulas (BE-05). */
class AdminContentControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String CONTENTS_URL = "/api/v1/admin/modules/{moduleId}/contents";
    private static final String VIDEO_BODY = """
            {"title": "Aula 1", "type": "VIDEO", "url": "https://youtu.be/x", "durationMinutes": 5, "isFree": true}
            """;
    private static final String UPDATED_VIDEO_BODY = """
            {"title": "Aula 1b", "type": "VIDEO", "url": "https://youtu.be/y", "durationMinutes": 6, "isFree": false}
            """;

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryContentRepositoryPort contentRepository = new InMemoryContentRepositoryPort();

    private String adminId;
    private String studentId;
    private UUID moduleId;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN")).getId().toString();
        studentId = userRepository.save(new User("Aluno Um", "aluno@escapa.com", "hash", "STUDENT")).getId().toString();
        moduleId = moduleRepository.createModule();

        final AdminContentController controller = new AdminContentController(
                new CreateContentUseCase(contentRepository, moduleRepository),
                new UpdateContentUseCase(contentRepository),
                new GetContentUseCase(contentRepository, moduleRepository),
                new ListModuleContentsUseCase(contentRepository, moduleRepository),
                new DeleteContentUseCase(contentRepository),
                new ReorderContentsUseCase(contentRepository, moduleRepository),
                new AdminRequestGuard(new AuthorizeAdminUseCase(userRepository))
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldRejectRequestsWithoutTheUserHeader() throws Exception {
        mockMvc.perform(get(CONTENTS_URL, moduleId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Missing X-User-Id header"));
    }

    @Test
    void shouldRejectNonAdminUsers() throws Exception {
        mockMvc.perform(get(CONTENTS_URL, moduleId).header(USER_HEADER, studentId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: user is not ADMIN"));
    }

    @Test
    void shouldRejectMalformedUserHeader() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/contents/{id}", UUID.randomUUID()).header(USER_HEADER, "not-a-uuid"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid X-User-Id: must be a valid UUID"));
    }

    @Test
    void shouldCreateAndListContentsInsideTheEnvelope() throws Exception {
        mockMvc.perform(post(CONTENTS_URL, moduleId).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(VIDEO_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Content created successfully"))
                .andExpect(jsonPath("$.data.title").value("Aula 1"))
                .andExpect(jsonPath("$.data.order").value(1));

        mockMvc.perform(get(CONTENTS_URL, moduleId).header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldRejectInvalidBodyWith400() throws Exception {
        mockMvc.perform(post(CONTENTS_URL, moduleId).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateAndDeleteAContent() throws Exception {
        final String created = mockMvc.perform(post(CONTENTS_URL, moduleId).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(VIDEO_BODY))
                .andReturn().getResponse().getContentAsString();
        final String contentId = created.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/v1/admin/contents/{id}", contentId).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATED_VIDEO_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Aula 1b"));

        mockMvc.perform(delete("/api/v1/admin/contents/{id}", contentId).header(USER_HEADER, adminId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(CONTENTS_URL + "/{id}", moduleId, contentId).header(USER_HEADER, adminId))
                .andExpect(status().isNotFound());
    }
}
