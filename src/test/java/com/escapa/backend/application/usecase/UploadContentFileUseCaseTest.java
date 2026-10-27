package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.InvalidContentFileException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadContentFileUseCaseTest {

    private final InMemoryFileStoragePort fileStorage = new InMemoryFileStoragePort();
    private final UploadContentFileUseCase useCase = new UploadContentFileUseCase(fileStorage);

    private InputStream fakeFile() {
        return new ByteArrayInputStream(new byte[]{1, 2, 3});
    }

    @Test
    void shouldUploadFileAndReturnItsPublicUrl() {
        final String url = useCase.execute("Apostila.PDF", fakeFile(), 1024);

        assertEquals(1, fileStorage.getUploadedKeys().size());
        final String uploadedKey = fileStorage.getUploadedKeys().get(0);
        assertTrue(uploadedKey.startsWith("contents/"));
        assertTrue(uploadedKey.endsWith(".pdf"));
        assertEquals("https://storage.test/" + uploadedKey, url);
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        assertThrows(InvalidContentFileException.class, () -> useCase.execute("script.exe", fakeFile(), 1024));
        assertThrows(InvalidContentFileException.class, () -> useCase.execute("sem-extensao", fakeFile(), 1024));
        assertTrue(fileStorage.getUploadedKeys().isEmpty());
    }

    @Test
    void shouldRejectFileLargerThanLimit() {
        final long tooLarge = UploadContentFileUseCase.MAX_FILE_SIZE_BYTES + 1;

        assertThrows(InvalidContentFileException.class, () -> useCase.execute("planilha.xlsx", fakeFile(), tooLarge));
        assertTrue(fileStorage.getUploadedKeys().isEmpty());
    }
}
