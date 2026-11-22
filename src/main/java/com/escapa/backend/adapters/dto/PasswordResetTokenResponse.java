package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.PasswordResetTokenDetails;

public record PasswordResetTokenResponse(String email) {

    public static PasswordResetTokenResponse from(PasswordResetTokenDetails details) {
        return new PasswordResetTokenResponse(details.email());
    }
}
