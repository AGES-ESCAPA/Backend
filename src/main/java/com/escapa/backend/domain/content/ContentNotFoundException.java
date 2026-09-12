package com.escapa.backend.domain.content;

import java.util.UUID;

public class ContentNotFoundException extends RuntimeException {

    public ContentNotFoundException(UUID id) {
        super("Content not found: " + id);
    }
}
