package com.escapa.backend.domain.user;

import java.util.UUID;

public class ProfileNotEditableException extends RuntimeException {

    public ProfileNotEditableException(UUID userId) {
        super("Profile cannot be edited by this user: " + userId);
    }
}
