package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.usecase.UploadAvatarUseCase;
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

import java.io.InputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvatarControllerTest {

    private UploadAvatarUseCase uploadAvatarUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.uploadAvatarUseCase = mock(UploadAvatarUseCase.class);
        this.mockMvc = MockMvcBuilders.standaloneSetup(new AvatarController(uploadAvatarUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUploadAvatarForTheIdentifiedStudent() throws Exception {
        final UUID userId = UUID.randomUUID();
        authenticate(userId);
        final byte[] content = {1, 2, 3};
        final MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", content);
        final User updatedUser = new User();
        updatedUser.setAvatarUrl("https://media.example/avatars/new.png");
        when(uploadAvatarUseCase.execute(eq(userId), any(InputStream.class), eq((long) content.length), eq("image/png")))
                .thenReturn(updatedUser);

        mockMvc.perform(multipart("/api/v1/me/avatar")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://media.example/avatars/new.png"));

        verify(uploadAvatarUseCase).execute(eq(userId), any(InputStream.class), eq(3L), eq("image/png"));
    }

    @Test
    void shouldRejectRequestWithoutAuthenticatedUser() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1});

        mockMvc.perform(multipart("/api/v1/me/avatar")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(uploadAvatarUseCase);
    }

    @Test
    void shouldRejectRequestWithAnUnexpectedAuthenticationPrincipal() throws Exception {
        final MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] {1});
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-an-authenticated-user", null));

        mockMvc.perform(multipart("/api/v1/me/avatar")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(uploadAvatarUseCase);
    }

    private void authenticate(UUID userId) {
        final AuthenticatedUser user = new AuthenticatedUser(userId, "student@escapa.com", "STUDENT", "STUDENT");
        final var authentication = new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_STUDENT"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
