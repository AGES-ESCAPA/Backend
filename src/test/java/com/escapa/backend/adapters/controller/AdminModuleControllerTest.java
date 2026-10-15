package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.CreateModuleUseCase;
import com.escapa.backend.application.usecase.DeleteModuleUseCase;
import com.escapa.backend.application.usecase.InMemoryCourseRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryModuleRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.ListCourseModulesUseCase;
import com.escapa.backend.application.usecase.ReorderModulesUseCase;
import com.escapa.backend.application.usecase.UpdateModuleUseCase;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP e controle de acesso ADMIN dos módulos (US-06). */
class AdminModuleControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String MODULES_URL = "/api/v1/admin/courses/{courseId}/modules";

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();

    private String adminId;
    private UUID courseId;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN")).getId().toString();
        courseId = courseRepository.save(new Course()).getId();

        final AdminModuleController controller = new AdminModuleController(
                new ListCourseModulesUseCase(moduleRepository, courseRepository),
                new CreateModuleUseCase(moduleRepository, courseRepository),
                new UpdateModuleUseCase(moduleRepository),
                new ReorderModulesUseCase(moduleRepository, courseRepository),
                new DeleteModuleUseCase(moduleRepository),
                new AdminRequestGuard(new AuthorizeAdminUseCase(userRepository))
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldRejectRequestsWithoutTheUserHeader() throws Exception {
        mockMvc.perform(get(MODULES_URL, courseId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Missing X-User-Id header"));
    }

    @Test
    void shouldCreateModulesAtTheEndOfTheCourse() throws Exception {
        createModule("Modulo 1").andExpect(jsonPath("$.data.order").value(1));
        createModule("Modulo 2")
                .andExpect(jsonPath("$.message").value("Module created successfully"))
                .andExpect(jsonPath("$.data.order").value(2));

        mockMvc.perform(get(MODULES_URL, courseId).header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void shouldRejectBlankTitleWith400() throws Exception {
        mockMvc.perform(post(MODULES_URL, courseId).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is required"));
    }

    @Test
    void shouldReturn404ForAnUnknownCourse() throws Exception {
        mockMvc.perform(get(MODULES_URL, UUID.randomUUID()).header(USER_HEADER, adminId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRenameReorderAndDeleteModules() throws Exception {
        final UUID first = moduleRepository.createModule(courseId);
        final UUID second = moduleRepository.createModule(courseId);

        mockMvc.perform(put("/api/v1/admin/modules/{id}", first).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Renomeado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Renomeado"));

        mockMvc.perform(put(MODULES_URL + "/reorder", courseId).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moduleIds\": [\"" + second + "\", \"" + first + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(second.toString()));

        mockMvc.perform(delete("/api/v1/admin/modules/{id}", first).header(USER_HEADER, adminId))
                .andExpect(status().isNoContent());
    }

    private ResultActions createModule(String title) throws Exception {
        return mockMvc.perform(post(MODULES_URL, courseId).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"" + title + "\"}"))
                .andExpect(status().isCreated());
    }
}
