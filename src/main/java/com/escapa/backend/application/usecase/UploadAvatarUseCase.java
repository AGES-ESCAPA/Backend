package com.escapa.backend.application.usecase;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import com.escapa.backend.application.port.FileStoragePort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InvalidAvatarFileException;
import com.escapa.backend.domain.user.UserNotFoundException;

public class UploadAvatarUseCase {

    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final UserRepositoryPort userRepositoryPort;
    private final FileStoragePort fileStoragePort;

    public UploadAvatarUseCase(UserRepositoryPort userRepositoryPort, FileStoragePort fileStoragePort) {
        this.userRepositoryPort = userRepositoryPort;
        this.fileStoragePort = fileStoragePort;
    }

    public User execute(UUID userId, InputStream content, long size, String contentType) {
        validate(contentType, size);

        final User user = userRepositoryPort.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        final String key = "avatars/" + UUID.randomUUID() + "." + ALLOWED_CONTENT_TYPES.get(contentType);
        final String newAvatarUrl = fileStoragePort.upload(key, content, size, contentType);
        final String previousAvatarUrl = user.getAvatarUrl();

        user.setAvatarUrl(newAvatarUrl);
        final User savedUser = userRepositoryPort.save(user);

        if (previousAvatarUrl != null) {
            fileStoragePort.delete(previousAvatarUrl);
        }

        return savedUser;
    }

    private void validate(String contentType, long size) {
        if (!ALLOWED_CONTENT_TYPES.containsKey(contentType)) {
            throw InvalidAvatarFileException.unsupportedType(contentType);
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw InvalidAvatarFileException.tooLarge(MAX_FILE_SIZE_BYTES);
        }
    }
}