package com.escapa.backend.adapters.exception;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error body returned for every failed request")
public record ApiError(int status, String error, String message, String path, Instant timestamp) {
}
