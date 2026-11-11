package com.escapa.backend.adapters.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Result of resending the invite of a pending employee")
public record ResentInviteResponse(
        UUID id,
        @Schema(example = "joao.silva@empresa.com.br")
        String email,
        @Schema(description = "False when the e-mail failed to send; the manager can try again", example = "true")
        boolean inviteSent
) {
}
