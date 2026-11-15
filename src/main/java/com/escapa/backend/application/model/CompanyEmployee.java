package com.escapa.backend.application.model;

import com.escapa.backend.domain.user.UserStatus;

import java.util.UUID;

/**
 * Colaborador vinculado a uma empresa, como aparece na Gestão de Colaboradores (US-30).
 *
 * @param jobTitle        cargo, guardado em {@code users_company.role}
 * @param assignedCourses matrículas não revogadas nos cursos da empresa
 * @param avatarUrl       nulo quando o colaborador não tem foto
 */
public record CompanyEmployee(
        UUID id,
        String name,
        String email,
        String jobTitle,
        UserStatus status,
        int assignedCourses,
        String avatarUrl
) {
}
