package com.escapa.backend.domain.notification;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("Notification not found: " + id);
    }
}
