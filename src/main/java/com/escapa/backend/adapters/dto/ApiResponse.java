package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Success envelope: success flag, payload and a human-readable message")
public record ApiResponse<T>(boolean success, T data, String message) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "Operation completed successfully");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }
}
