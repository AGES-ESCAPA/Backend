package com.escapa.backend.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MinioFileStorageAdapterTest {

    private static final String BUCKET = "media.escapaages.online";
    private static final String BASE_URL = "https://media.escapaages.online";
    private static final String KEY = "courses/thumbnails/a.png";

    private final MinioClient minioClient = mock(MinioClient.class);

    private String upload(MinioFileStorageAdapter adapter) {
        return adapter.upload(KEY, new ByteArrayInputStream(new byte[]{1}), 1, "image/png");
    }

    @Test
    void shouldIncludeBucketInPublicUrlByDefault() {
        final MinioFileStorageAdapter adapter = new MinioFileStorageAdapter(minioClient, BUCKET, BASE_URL);

        assertEquals(BASE_URL + "/" + BUCKET + "/" + KEY, upload(adapter));
    }

    @Test
    void shouldOmitBucketFromPublicUrlWhenBucketIsResolvedByHostname() {
        final MinioFileStorageAdapter adapter = new MinioFileStorageAdapter(minioClient, BUCKET, BASE_URL, false);

        assertEquals(BASE_URL + "/" + KEY, upload(adapter));
    }

    @Test
    void shouldUploadToConfiguredBucketRegardlessOfPublicUrlShape() throws Exception {
        final MinioFileStorageAdapter adapter = new MinioFileStorageAdapter(minioClient, BUCKET, BASE_URL, false);

        upload(adapter);

        final ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals(KEY, captor.getValue().object());
    }

    @Test
    void shouldExtractKeyFromUrlWithoutBucketOnDelete() throws Exception {
        final MinioFileStorageAdapter adapter = new MinioFileStorageAdapter(minioClient, BUCKET, BASE_URL, false);

        adapter.delete(BASE_URL + "/" + KEY);

        final ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals(KEY, captor.getValue().object());
    }

    @Test
    void shouldExtractKeyFromUrlWithBucketOnDelete() throws Exception {
        final MinioFileStorageAdapter adapter = new MinioFileStorageAdapter(minioClient, BUCKET, BASE_URL, true);

        adapter.delete(BASE_URL + "/" + BUCKET + "/" + KEY);

        final ArgumentCaptor<RemoveObjectArgs> captor = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(minioClient).removeObject(captor.capture());
        assertEquals(KEY, captor.getValue().object());
    }
}
