package com.escapa.backend.adapters.exception;

import java.time.Instant;

public record ApiError(int status, String error, String message, String path, Instant timestamp) {
}
