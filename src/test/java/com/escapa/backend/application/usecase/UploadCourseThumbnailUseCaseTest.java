package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.InvalidCourseThumbnailException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadCourseThumbnailUseCaseTest {

    private final InMemoryFileStoragePort fileStorage = new InMemoryFileStoragePort();
    private final UploadCourseThumbnailUseCase useCase = new UploadCourseThumbnailUseCase(fileStorage);

    private InputStream fakeFile() {
        return new ByteArrayInputStream(new byte[]{1, 2, 3});
    }

    @Test
    void shouldUploadImageAndReturnItsPublicUrl() {
        final String url = useCase.execute(fakeFile(), 1024, "image/png");

        assertEquals(1, fileStorage.getUploadedKeys().size());
        final String uploadedKey = fileStorage.getUploadedKeys().get(0);
        assertTrue(uploadedKey.startsWith("courses/thumbnails/"));
        assertTrue(uploadedKey.endsWith(".png"));
        assertEquals("https://storage.test/" + uploadedKey, url);
    }

    @Test
    void shouldAcceptJpegAndWebp() {
        useCase.execute(fakeFile(), 1024, "image/jpeg");
        useCase.execute(fakeFile(), 1024, "image/webp");

        assertTrue(fileStorage.getUploadedKeys().get(0).endsWith(".jpg"));
        assertTrue(fileStorage.getUploadedKeys().get(1).endsWith(".webp"));
    }

    @Test
    void shouldRejectUnsupportedType() {
        assertThrows(InvalidCourseThumbnailException.class, () -> useCase.execute(fakeFile(), 1024, "image/gif"));
        assertThrows(InvalidCourseThumbnailException.class, () -> useCase.execute(fakeFile(), 1024, null));
        assertTrue(fileStorage.getUploadedKeys().isEmpty());
    }

    @Test
    void shouldRejectFileLargerThanLimit() {
        final long tooLarge = UploadCourseThumbnailUseCase.MAX_FILE_SIZE_BYTES + 1;

        assertThrows(InvalidCourseThumbnailException.class, () -> useCase.execute(fakeFile(), tooLarge, "image/jpeg"));
        assertTrue(fileStorage.getUploadedKeys().isEmpty());
    }
}
