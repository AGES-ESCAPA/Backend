package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ChangePasswordRequest;
import com.escapa.backend.application.usecase.ChangePasswordUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Troca de senha do usuario logado, para qualquer perfil. Ate a autenticacao
 * por token (#41) entrar, o usuario vem do header provisorio {@code X-User-Id}.
 */
@RestController
@RequestMapping("/api/v1/me")
public class PasswordController {

    private final ChangePasswordUseCase changePasswordUseCase;

    public PasswordController(ChangePasswordUseCase changePasswordUseCase) {
        this.changePasswordUseCase = changePasswordUseCase;
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        final UUID userId = requireUserId(xUserId);
        changePasswordUseCase.execute(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    private UUID requireUserId(String xUserId) {
        if (xUserId == null || xUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        try {
            return UUID.fromString(xUserId.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid X-User-Id: must be a valid UUID");
        }
    }
}
