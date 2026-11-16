package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.AccountInviteDetails;

public record AccountInviteResponse(
        String name,
        String email,
        String companyName
) {

    public static AccountInviteResponse from(AccountInviteDetails details) {
        return new AccountInviteResponse(details.name(), details.email(), details.companyName());
    }
}
