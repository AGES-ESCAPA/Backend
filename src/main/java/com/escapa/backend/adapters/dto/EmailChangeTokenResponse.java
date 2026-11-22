package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.EmailChangeTokenDetails;

public record EmailChangeTokenResponse(String name, String currentEmail, String newEmail) {

    public static EmailChangeTokenResponse from(EmailChangeTokenDetails details) {
        return new EmailChangeTokenResponse(details.name(), details.currentEmail(), details.newEmail());
    }
}
