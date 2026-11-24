package com.escapa.backend.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class FileStorageExceptionTest {

    @Test
    void shouldKeepMessageAndCause() {
        final RuntimeException cause = new RuntimeException("minio down");
        final FileStorageException error = new FileStorageException("Failed to upload", cause);

        assertEquals("Failed to upload", error.getMessage());
        assertSame(cause, error.getCause());
    }
}
