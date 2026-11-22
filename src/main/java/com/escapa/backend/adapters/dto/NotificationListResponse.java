package com.escapa.backend.adapters.dto;

import java.util.List;

import com.escapa.backend.application.usecase.ListUserNotificationsUseCase;

public record NotificationListResponse(List<NotificationResponse> notifications, long unreadCount) {

    public static NotificationListResponse from(ListUserNotificationsUseCase.Result result) {
        return new NotificationListResponse(
                result.notifications().stream().map(NotificationResponse::from).toList(),
                result.unreadCount());
    }
}
