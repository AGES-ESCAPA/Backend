package com.escapa.backend.adapters.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.InMemoryFileStoragePort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.UploadContentFileUseCase;
import com.escapa.backend.domain.entity.User;

class AdminContentFileControllerTest {

    private static final String FILES_URL = "/api/v1/admin/contents/files";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private MockMvc mockMvc;
    private String adminId;

    @BeforeEach
    void setUp() {
        adminId = users.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN")).getId().toString();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminContentFileController(
                        new UploadContentFileUseCase(new InMemoryFileStoragePort()),
                        new AdminRequestGuard(new AuthorizeAdminUseCase(users))))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate(adminId);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(String userId) {
        final var user = new AuthenticatedUser(UUID.fromString(userId), "admin@escapa.com", "ADMIN", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @Test
    void shouldUploadContentFileAndReturnItsPublicUrl() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file", "material.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(FILES_URL).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url", startsWith("https://storage.test/contents/")))
                .andExpect(jsonPath("$.data.url", endsWith(".pdf")));
    }

    @Test
    void shouldRejectUnsupportedFileExtension() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(FILES_URL).file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNonAdminUser() throws Exception {
        final UUID studentId = users.save(new User("Aluno", "aluno@email.com", "hash", "STUDENT")).getId();
        authenticate(studentId.toString());
        final MockMultipartFile file = new MockMultipartFile(
                "file", "material.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(FILES_URL).file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldWrapInputStreamFailures() throws Exception {
        final MultipartFile broken = mock(MultipartFile.class);
        when(broken.getOriginalFilename()).thenReturn("material.pdf");
        when(broken.getSize()).thenReturn(3L);
        when(broken.getInputStream()).thenThrow(new IOException("disk failed"));
        final AdminContentFileController controller = new AdminContentFileController(
                new UploadContentFileUseCase(new InMemoryFileStoragePort()),
                new AdminRequestGuard(new AuthorizeAdminUseCase(users)));

        final UncheckedIOException error = assertThrows(
                UncheckedIOException.class,
                () -> controller.upload(broken));

        assertEquals("Failed to read uploaded file", error.getMessage());
        assertInstanceOf(IOException.class, error.getCause());
    }
}
