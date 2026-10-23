package com.escapa.backend.infrastructure.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioFileStorageAdapterTest {

    private MinioClient minioClient;
    private MinioFileStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        this.minioClient = mock(MinioClient.class);
        this.adapter = new MinioFileStorageAdapter(minioClient, "escapa-media", "https://media.example");
    }

    @Test
    void shouldUploadObjectAndReturnPublicUrl() throws Exception {
        final ByteArrayInputStream content = new ByteArrayInputStream(new byte[] {1, 2, 3});
        final String url = adapter.upload("avatars/user.png", content, 3, "image/png");
        final ArgumentCaptor<PutObjectArgs> arguments = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(arguments.capture());

        assertEquals("https://media.example/escapa-media/avatars/user.png", url);
        assertEquals("escapa-media", arguments.getValue().bucket());
        assertEquals("avatars/user.png", arguments.getValue().object());
        assertEquals("image/png", arguments.getValue().contentType().toString());
        assertEquals(3, arguments.getValue().objectSize());
    }

    @Test
    void shouldDeleteObjectUsingKeyFromItsPublicUrl() throws Exception {
        adapter.delete("https://media.example/escapa-media/avatars/user.png");
        final ArgumentCaptor<RemoveObjectArgs> arguments = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(arguments.capture());

        assertEquals("escapa-media", arguments.getValue().bucket());
        assertEquals("avatars/user.png", arguments.getValue().object());
    }

    @Test
    void shouldDeleteAKeyWhenTheInputIsNotAPublicUrl() throws Exception {
        adapter.delete("avatars/user.png");
        final ArgumentCaptor<RemoveObjectArgs> arguments = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(arguments.capture());

        assertEquals("avatars/user.png", arguments.getValue().object());
    }

    @Test
    void shouldWrapUploadFailures() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new IllegalStateException("unavailable"));

        final FileStorageException exception = assertThrows(FileStorageException.class,
                () -> adapter.upload("avatars/user.png", new ByteArrayInputStream(new byte[] {1}),
                        1, "image/png"));

        assertEquals("Failed to upload file to storage", exception.getMessage());
        assertEquals("unavailable", exception.getCause().getMessage());
    }

    @Test
    void shouldWrapDeleteFailures() throws Exception {
        doThrow(new IllegalStateException("unavailable"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        final FileStorageException exception = assertThrows(FileStorageException.class,
                () -> adapter.delete("avatars/user.png"));

        assertEquals("Failed to delete file from storage", exception.getMessage());
        assertEquals("unavailable", exception.getCause().getMessage());
    }
}
