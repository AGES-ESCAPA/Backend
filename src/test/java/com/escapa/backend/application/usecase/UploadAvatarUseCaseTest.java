package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InvalidAvatarFileException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadAvatarUseCaseTest {

    private final UserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryFileStoragePort fileStorage = new InMemoryFileStoragePort();
    private final UploadAvatarUseCase useCase = new UploadAvatarUseCase(userRepository, fileStorage);

    private User createUser() {
        return userRepository.save(new User("Maria Silva", "maria@email.com", "hash", "STUDENT"));
    }

    private InputStream fakeImage() {
        return new ByteArrayInputStream(new byte[]{1, 2, 3});
    }

    @Test
    void shouldUploadAvatarAndUpdateUser() {
        final User user = createUser();

        final User updated = useCase.execute(user.getId(), fakeImage(), 1024, "image/png");

        assertEquals(1, fileStorage.getUploadedKeys().size());
        final String uploadedKey = fileStorage.getUploadedKeys().get(0);
        assertTrue(uploadedKey.startsWith("avatars/"));
        assertTrue(uploadedKey.endsWith(".png"));
        assertEquals("https://storage.test/" + uploadedKey, updated.getAvatarUrl());
    }

    @Test
    void shouldRejectUnsupportedFileType() {
        final User user = createUser();

        assertThrows(
                InvalidAvatarFileException.class,
                () -> useCase.execute(user.getId(), fakeImage(), 1024, "application/pdf")
        );
        assertTrue(fileStorage.getUploadedKeys().isEmpty());
    }

    @Test
    void shouldRejectFileLargerThanTwoMegabytes() {
        final User user = createUser();
        final long tooLarge = 2L * 1024 * 1024 + 1;

        assertThrows(
                InvalidAvatarFileException.class,
                () -> useCase.execute(user.getId(), fakeImage(), tooLarge, "image/png")
        );
        assertTrue(fileStorage.getUploadedKeys().isEmpty());
    }

    @Test
    void shouldDeletePreviousAvatarWhenReplacingPhoto() {
        final User user = createUser();
        final User afterFirstUpload = useCase.execute(user.getId(), fakeImage(), 1024, "image/jpeg");
        final String firstAvatarUrl = afterFirstUpload.getAvatarUrl();

        final User afterSecondUpload = useCase.execute(user.getId(), fakeImage(), 1024, "image/webp");

        assertEquals(2, fileStorage.getUploadedKeys().size());
        assertEquals(1, fileStorage.getDeletedUrls().size());
        assertEquals(firstAvatarUrl, fileStorage.getDeletedUrls().get(0));
        assertTrue(afterSecondUpload.getAvatarUrl().endsWith(".webp"));
    }
}