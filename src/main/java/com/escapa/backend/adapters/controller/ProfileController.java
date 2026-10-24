package com.escapa.backend.adapters.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ProfileResponse;
import com.escapa.backend.adapters.dto.ProfileUpdateRequest;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.GetProfileUseCase;
import com.escapa.backend.application.usecase.UpdateProfileUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Tag(name = "Me - Profile", description = "Profile data of the authenticated user")
// Define o prefixo comum das rotas de consulta e atualização do perfil.
@RequestMapping("/api/v1/me")
public class ProfileController {

    // Serviço responsável por buscar e atualizar os dados do perfil.
    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;

    // O Spring injeta automaticamente o serviço utilizado pelo controller.
    public ProfileController(GetProfileUseCase getProfileUseCase, UpdateProfileUseCase updateProfileUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
    }

    // Busca o perfil do usuário identificado pelo token.
    @GetMapping("/profile")
    @Operation(summary = "Get the profile data of the logged-in user")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        final UUID userId = AuthenticationContext.requireUser().id();
        return ResponseEntity.ok(ApiResponse.success(ProfileResponse.from(getProfileUseCase.execute(userId))));
    }

    // Recebe os novos dados do perfil e devolve o perfil atualizado.
    @PutMapping("/profile")
    @Operation(summary = "Update the editable profile data of the logged-in user (403 for company employees)")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        final UUID userId = AuthenticationContext.requireUser().id();

        // Retorna HTTP 200 com os dados atualizados no corpo da resposta.
        return ResponseEntity.ok(ApiResponse.success(ProfileResponse.from(updateProfileUseCase.execute(
                userId, request.name(), request.phone(), request.companyName()))));
    }
}
