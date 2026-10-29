package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.usecase.ListUserNotificationsUseCase;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> notifications, long unreadCount) {

    public static NotificationListResponse from(ListUserNotificationsUseCase.Result result) {
        return new NotificationListResponse(
                result.notifications().stream().map(NotificationResponse::from).toList(),
                result.unreadCount());
    }
}
