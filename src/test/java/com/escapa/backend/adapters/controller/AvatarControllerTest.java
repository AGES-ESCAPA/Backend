package com.escapa.backend.adapters.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.usecase.InMemoryFileStoragePort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.UploadAvatarUseCase;
import com.escapa.backend.domain.entity.User;

class AvatarControllerTest {

    private static final String AVATAR_URL = "/api/v1/me/avatar";

    private final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
    private MockMvc mockMvc;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        studentId = users.save(new User("Mariana", "mariana@email.com", "hash", "STUDENT")).getId();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AvatarController(new UploadAvatarUseCase(users, new InMemoryFileStoragePort())))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate(studentId, "STUDENT");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(UUID userId, String role) {
        final var user = new AuthenticatedUser(userId, "test@escapa.com", role, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Test
    void shouldUploadAvatarAndReturnItsPublicUrl() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(AVATAR_URL).file(file).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatarUrl", startsWith("https://storage.test/avatars/")))
                .andExpect(jsonPath("$.data.avatarUrl", endsWith(".png")));

        final String avatarUrl = users.findById(studentId).orElseThrow().getAvatarUrl();
        assertNotNull(avatarUrl);
        assertTrue(avatarUrl.startsWith("https://storage.test/avatars/"));
    }

    @Test
    void shouldRejectUnsupportedAvatarType() throws Exception {
        final MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.gif", "image/gif", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(AVATAR_URL).file(file).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type: image/gif"));
    }

    @Test
    void shouldRejectUnauthenticatedUpload() throws Exception {
        SecurityContextHolder.clearContext();
        final MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(AVATAR_URL).file(file).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldWrapInputStreamFailures() throws Exception {
        final MultipartFile broken = mock(MultipartFile.class);
        when(broken.getInputStream()).thenThrow(new IOException("disk failed"));
        final AvatarController controller = new AvatarController(
                new UploadAvatarUseCase(users, new InMemoryFileStoragePort()));

        final UncheckedIOException error = assertThrows(
                UncheckedIOException.class,
                () -> controller.uploadAvatar(broken));

        assertEquals("Failed to read uploaded file", error.getMessage());
        assertInstanceOf(IOException.class, error.getCause());
    }
}
