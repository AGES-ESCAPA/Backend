package com.escapa.backend.adapters.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ProfileResponseDTO;
import com.escapa.backend.adapters.dto.ProfileUpdateDTO;

@RestController
// Define o prefixo comum das rotas de consulta e atualização do perfil.
@RequestMapping("/api/v1/me/profile")
public class ProfileController {

    // Serviço responsável por buscar e atualizar os dados do perfil.
    private final ProfileService profileService;

    // O Spring injeta automaticamente o serviço utilizado pelo controller.
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // Busca o perfil do usuário identificado pelo UUID informado na URL.
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> getProfile(Principal authentication) {
        final UUID userId = UUID.fromString(authentication.getName());
        final ProfileResponseDTO response = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Recebe os novos dados do perfil e devolve o perfil atualizado.
    @PutMapping
    public ResponseEntity<ProfileResponseDTO> updateProfile(
            @PathVariable UUID userId,
            @RequestBody ProfileUpdateDTO updateDTO
    ) {
        final ProfileResponseDTO response = profileService.updateProfile(userId, updateDTO);

        // Retorna HTTP 200 com os dados atualizados no corpo da resposta.
        return ResponseEntity.ok(response);
    }
}
