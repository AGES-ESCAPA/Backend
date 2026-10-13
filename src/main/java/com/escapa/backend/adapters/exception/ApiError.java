package com.escapa.backend.adapters.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standard error body returned for every failed request")
public record ApiError(int status, String error, String message, String path, Instant timestamp) {
}
