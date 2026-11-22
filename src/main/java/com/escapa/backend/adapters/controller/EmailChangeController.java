package com.escapa.backend.adapters.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.EmailChangeTokenResponse;
import com.escapa.backend.application.usecase.ConfirmEmailChangeUseCase;
import com.escapa.backend.application.usecase.GetEmailChangeTokenUseCase;

@RestController
@RequestMapping("/api/v1/auth/email-change")
@Tag(name = "Authentication - Email change", description = "Confirm a pending e-mail change via e-mail link")
public class EmailChangeController {

    private final GetEmailChangeTokenUseCase getEmailChangeTokenUseCase;
    private final ConfirmEmailChangeUseCase confirmEmailChangeUseCase;

    public EmailChangeController(
            GetEmailChangeTokenUseCase getEmailChangeTokenUseCase,
            ConfirmEmailChangeUseCase confirmEmailChangeUseCase
    ) {
        this.getEmailChangeTokenUseCase = getEmailChangeTokenUseCase;
        this.confirmEmailChangeUseCase = confirmEmailChangeUseCase;
    }

    @GetMapping("/{token}")
    @Operation(
            summary = "Validate an email-change token",
            description = "Unknown or used token returns 404; expired token returns 410."
    )
    public ResponseEntity<ApiResponse<EmailChangeTokenResponse>> getToken(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(
                EmailChangeTokenResponse.from(getEmailChangeTokenUseCase.execute(token))));
    }

    @PostMapping("/{token}/confirm")
    @Operation(
            summary = "Confirm the email change with a valid token",
            description = "Applies the new e-mail only after this confirmation."
    )
    public ResponseEntity<ApiResponse<Void>> confirm(@PathVariable String token) {
        confirmEmailChangeUseCase.execute(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email updated successfully"));
    }
}
