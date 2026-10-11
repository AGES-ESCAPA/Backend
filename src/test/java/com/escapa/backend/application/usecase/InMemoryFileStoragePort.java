package com.escapa.backend.application.usecase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.escapa.backend.application.port.FileStoragePort;

public final class InMemoryFileStoragePort implements FileStoragePort {

    private final List<String> uploadedKeys = new ArrayList<>();
    private final List<String> deletedUrls = new ArrayList<>();

    @Override
    public String upload(String key, InputStream content, long size, String contentType) {
        uploadedKeys.add(key);
        return "https://storage.test/" + key;
    }

    @Override
    public void delete(String url) {
        deletedUrls.add(url);
    }

    public List<String> getUploadedKeys() {
        return List.copyOf(uploadedKeys);
    }

    public List<String> getDeletedUrls() {
        return List.copyOf(deletedUrls);
    }
}