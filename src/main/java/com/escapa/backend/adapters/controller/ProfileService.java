package com.escapa.backend.adapters.controller;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.escapa.backend.adapters.dto.ProfileResponseDTO;
import com.escapa.backend.adapters.dto.ProfileUpdateDTO;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;

@Service
public class ProfileService {
    // Porta que permite acessar os usuários sem acoplar o serviço ao JPA.
    private final UserRepositoryPort userRepositoryPort;

    // Recebe a implementação da porta fornecida pela infraestrutura.
    public ProfileService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    // Consulta um usuário e transforma o domínio no DTO devolvido pela API.
    public ProfileResponseDTO getProfile(UUID userId) {
        return mapToResponse(findUser(userId));
    }

    // Atualiza os dados básicos do perfil: nome e endereço de e-mail.
    public ProfileResponseDTO updateProfile(UUID userId, ProfileUpdateDTO dto) {
        final User user = findUser(userId);

        // Usuários do tipo EMPLOYEE podem visualizar, mas não editar o perfil.
        if ("EMPLOYEE".equals(user.getUserType())) {
            throw new IllegalArgumentException("Empregados não podem atualizar o perfil.");
        }

        // Copia os dados recebidos para a entidade de domínio antes de salvar.
        user.setName(dto.name());
        user.setEmail(dto.email());

        // Persiste o usuário e usa o objeto salvo para montar a resposta final.
        final User savedUser = userRepositoryPort.save(user);
        return mapToResponse(savedUser);
    }

    // Centraliza a busca e mantém o erro de usuário inexistente padronizado.
    private User findUser(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // Converte a entidade de domínio para o formato exposto pela API.
    private ProfileResponseDTO mapToResponse(User user) {
        // STUDENT e COMPANY podem editar o perfil; EMPLOYEE pode apenas consultá-lo.
        final boolean isEditable = !"EMPLOYEE".equals(user.getUserType());

        return new ProfileResponseDTO(
                user.getUserType(),
                isEditable,
                user.getName(),
                user.getEmail(),
                // Estes dados ainda não existem na entidade User e permanecem nulos.
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
