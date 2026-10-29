package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.NotificationListResponse;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.ListUserNotificationsUseCase;
import com.escapa.backend.application.usecase.MarkAllNotificationsAsReadUseCase;
import com.escapa.backend.application.usecase.MarkNotificationAsReadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Notificacoes do usuario identificado pelo token Bearer da requisicao.
 */
@RestController
@Tag(name = "Notifications", description = "Notifications of the authenticated user")
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final ListUserNotificationsUseCase listUserNotificationsUseCase;
    private final MarkNotificationAsReadUseCase markNotificationAsReadUseCase;
    private final MarkAllNotificationsAsReadUseCase markAllNotificationsAsReadUseCase;

    public NotificationController(
            ListUserNotificationsUseCase listUserNotificationsUseCase,
            MarkNotificationAsReadUseCase markNotificationAsReadUseCase,
            MarkAllNotificationsAsReadUseCase markAllNotificationsAsReadUseCase
    ) {
        this.listUserNotificationsUseCase = listUserNotificationsUseCase;
        this.markNotificationAsReadUseCase = markNotificationAsReadUseCase;
        this.markAllNotificationsAsReadUseCase = markAllNotificationsAsReadUseCase;
    }

    @GetMapping
    @Operation(summary = "List the most recent notifications and the unread count")
    public ResponseEntity<ApiResponse<NotificationListResponse>> list() {
        final UUID userId = AuthenticationContext.requireUser().id();
        return ResponseEntity.ok(ApiResponse.success(
                NotificationListResponse.from(listUserNotificationsUseCase.execute(userId))));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        markNotificationAsReadUseCase.execute(AuthenticationContext.requireUser().id(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark every notification as read")
    public ResponseEntity<Void> markAllAsRead() {
        markAllNotificationsAsReadUseCase.execute(AuthenticationContext.requireUser().id());
        return ResponseEntity.noContent().build();
    }
}
