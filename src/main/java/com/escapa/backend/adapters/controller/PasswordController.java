package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ChangePasswordRequest;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.ChangePasswordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Troca de senha do usuario autenticado, para qualquer perfil.
 */
@RestController
@Tag(name = "Me - Password", description = "Password change of the authenticated user")
@RequestMapping("/api/v1/me")
public class PasswordController {

    private final ChangePasswordUseCase changePasswordUseCase;

    public PasswordController(ChangePasswordUseCase changePasswordUseCase) {
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @PutMapping("/password")
    @Operation(
            summary = "Change the password of the logged-in user",
            description = "Requires the current password. A wrong current password returns 400, not 401, "
                    + "so the frontend does not treat it as an expired session."
    )
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        final UUID userId = AuthenticationContext.requireUser().id();
        changePasswordUseCase.execute(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
}
