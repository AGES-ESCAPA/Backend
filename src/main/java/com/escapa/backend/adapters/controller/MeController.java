package com.escapa.backend.adapters.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.DeactivateAccountRequest;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.DeactivateAccountUseCase;

import jakarta.validation.Valid;

/**
 * Endpoints da conta do proprio usuario autenticado (US-24), identificado pelo token.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final DeactivateAccountUseCase deactivateAccountUseCase;

    public MeController(DeactivateAccountUseCase deactivateAccountUseCase) {
        this.deactivateAccountUseCase = deactivateAccountUseCase;
    }

    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @Valid @RequestBody DeactivateAccountRequest request
    ) {
        final UUID userId = AuthenticationContext.requireUser().id();
        deactivateAccountUseCase.execute(userId, request.currentPassword());
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Account deactivated successfully"));
    }
}