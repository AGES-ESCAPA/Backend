package com.escapa.backend.adapters.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.DeactivateAccountRequest;
import com.escapa.backend.application.usecase.DeactivateAccountUseCase;

import jakarta.validation.Valid;

/**
 * Endpoints da conta do proprio usuario autenticado (US-24). O usuario e
 * identificado pelo header provisorio {@code X-User-Id}, o mesmo das demais
 * rotas, ate o login da US-23 trazer o token.
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
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @Valid @RequestBody DeactivateAccountRequest request
    ) {
        final UUID userId = requireUserId(xUserId);
        deactivateAccountUseCase.execute(userId, request.currentPassword());
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Account deactivated successfully"));
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