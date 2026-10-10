package com.escapa.backend.infrastructure.storage;

import com.escapa.backend.application.port.FileStoragePort;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;

import java.io.InputStream;

public class MinioFileStorageAdapter implements FileStoragePort {

    private static final long UNKNOWN_PART_SIZE = -1;
    
    private final MinioClient minioClient;
    private final String bucket;
    private final String publicBaseUrl;

    public MinioFileStorageAdapter(MinioClient minioClient, String bucket, String publicBaseUrl) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String upload(String key, InputStream content, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(content, size, UNKNOWN_PART_SIZE)                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to upload file to storage", e);
        }
        return publicBaseUrl + "/" + bucket + "/" + key;
    }

    @Override
    public void delete(String url) {
        final String key = extractKey(url);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new FileStorageException("Failed to delete file from storage", e);
        }
    }

    private String extractKey(String url) {
        final String prefix = publicBaseUrl + "/" + bucket + "/";
        return url.startsWith(prefix) ? url.substring(prefix.length()) : url;
    }
}