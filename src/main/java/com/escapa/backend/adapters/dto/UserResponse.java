package com.escapa.backend.adapters.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        String name,
        String email,
        String userType,
        String status,
        LocalDateTime createdAt,
        String cpf,
        String phone,
        String companyName,
        String cnpj,
        /** E-mail aguardando confirmação do link; omitido quando não há troca pendente. */
        String pendingEmail
) {

    public static UserResponse from(User user) {
        return from(user, user.getUserType());
    }

    /** {@code userType} já resolvido (ex.: STUDENT vinculado à empresa → EMPLOYEE). */
    public static UserResponse from(User user, String userType) {
        final UserStatus status = user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE;
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                userType,
                status.name(),
                user.getCreatedAt(),
                null,
                null,
                null,
                null,
                null
        );
    }

    /** Detalhe administrativo com os dados de cadastro/perfil do usuário. */
    public static UserResponse from(User user, UserProfile profile) {
        return from(user, profile, null);
    }

    public static UserResponse from(User user, UserProfile profile, String pendingEmail) {
        final UserStatus status = user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE;
        return new UserResponse(
                user.getId(),
                profile.name(),
                profile.email(),
                profile.profile(),
                status.name(),
                user.getCreatedAt(),
                profile.cpf(),
                profile.phone(),
                profile.companyName(),
                profile.cnpj(),
                pendingEmail
        );
    }
}
