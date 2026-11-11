package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.InMemoryFileStoragePort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.UploadCourseThumbnailUseCase;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCourseThumbnailControllerTest {

    private static final String THUMBNAIL_URL = "/api/v1/admin/courses/thumbnail";

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private MockMvc mockMvc;
    private String adminId;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN")).getId().toString();
        final AdminCourseThumbnailController controller = new AdminCourseThumbnailController(
                new UploadCourseThumbnailUseCase(new InMemoryFileStoragePort()),
                new AdminRequestGuard(new AuthorizeAdminUseCase(userRepository))
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate(adminId);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUploadCoverAndReturnItsPublicUrl() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file", "capa.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(THUMBNAIL_URL).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url", startsWith("https://storage.test/courses/thumbnails/")))
                .andExpect(jsonPath("$.data.url", endsWith(".png")));
    }

    @Test
    void shouldRejectUnsupportedImageType() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file", "capa.gif", "image/gif", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(THUMBNAIL_URL).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type: image/gif"));
    }

    private static void authenticate(String userId) {
        final var user = new AuthenticatedUser(UUID.fromString(userId), "test@escapa.com", "ADMIN", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }
}
