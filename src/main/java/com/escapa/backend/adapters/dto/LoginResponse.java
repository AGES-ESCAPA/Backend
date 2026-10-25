package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.usecase.LoginOutput;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        LoginUserResponse user
) {

    public static LoginResponse from(LoginOutput output) {
        final LoginUserResponse user = new LoginUserResponse(
                output.user().getId(),
                output.user().getName(),
                output.user().getEmail(),
                output.profile(),
                output.user().getAvatarUrl()
        );
        return new LoginResponse(output.accessToken(), output.tokenType(), output.expiresIn(), user);
    }
}
