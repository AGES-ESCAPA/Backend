package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.UserProfile;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Profile data of the logged-in user, shaped by its profile "
        + "(STUDENT, EMPLOYEE, COMPANY or ADMIN)")
@JsonInclude(JsonInclude.Include.NON_NULL)
// Representa os dados enviados pela API ao consultar ou atualizar um perfil.
public record ProfileResponse(
        // Tipo do usuário, por exemplo STUDENT, COMPANY ou EMPLOYEE.
        String profile,
        // Indica se o usuário pode editar os próprios dados.
        boolean editable,
        // Dados básicos atualmente utilizados pelo perfil.
        @Schema(description = "Student/employee name, or the responsible person of a company")
        String name,
        String email,
        String cpf,
        String phone,
        @Schema(description = "Legal name of the company, or the employer of an employee")
        String companyName,
        String cnpj
) {

    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
                profile.profile(),
                profile.editable(),
                profile.name(),
                profile.email(),
                profile.cpf(),
                profile.phone(),
                profile.companyName(),
                profile.cnpj()
        );
    }
}
