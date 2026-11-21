package com.escapa.backend.adapters.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.PasswordResetTokenResponse;
import com.escapa.backend.adapters.dto.RequestPasswordResetRequest;
import com.escapa.backend.adapters.dto.ResetPasswordRequest;
import com.escapa.backend.application.usecase.GetPasswordResetTokenUseCase;
import com.escapa.backend.application.usecase.PasswordResetMailer;
import com.escapa.backend.application.usecase.RequestPasswordResetUseCase;
import com.escapa.backend.application.usecase.ResetPasswordUseCase;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
@Tag(name = "Authentication - Password reset", description = "Forgot password and reset via e-mail link")
public class PasswordResetController {

    private static final String REQUEST_MESSAGE =
            "Se o e-mail estiver cadastrado, enviaremos um link para redefinir a senha.";

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final GetPasswordResetTokenUseCase getPasswordResetTokenUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final PasswordResetMailer passwordResetMailer;

    public PasswordResetController(
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            GetPasswordResetTokenUseCase getPasswordResetTokenUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            PasswordResetMailer passwordResetMailer
    ) {
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.getPasswordResetTokenUseCase = getPasswordResetTokenUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.passwordResetMailer = passwordResetMailer;
    }

    @PostMapping("/request")
    @Operation(
            summary = "Request a password reset e-mail",
            description = "Always returns the same success message, whether or not the e-mail exists."
    )
    public ResponseEntity<ApiResponse<Void>> requestReset(@Valid @RequestBody RequestPasswordResetRequest request) {
        requestPasswordResetUseCase.execute(request.email()).ifPresent(command ->
                passwordResetMailer.send(command.name(), command.email(), command.token()));
        return ResponseEntity.ok(ApiResponse.success(null, REQUEST_MESSAGE));
    }

    @GetMapping("/{token}")
    @Operation(
            summary = "Validate a password reset token",
            description = "Unknown or used token returns 404; expired token returns 410."
    )
    public ResponseEntity<ApiResponse<PasswordResetTokenResponse>> getToken(@PathVariable String token) {
        final PasswordResetTokenResponse response =
                PasswordResetTokenResponse.from(getPasswordResetTokenUseCase.execute(token));
        return ResponseEntity.ok(ApiResponse.success(response, "Operation completed successfully"));
    }

    @PostMapping("/{token}")
    @Operation(
            summary = "Reset the password with a valid token",
            description = "Unknown or used token returns 404; expired token returns 410."
    )
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable String token,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        resetPasswordUseCase.execute(token, request.password());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }
}
