package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.port.UsersCompanyRepositoryPort;

public class GetProfileUseCase {

    private final UserProfileReader profileReader;

    public GetProfileUseCase(UserRepositoryPort userRepositoryPort,
                             CompanyRepositoryPort companyRepositoryPort,
                             UsersCompanyRepositoryPort usersCompanyRepositoryPort) {
        this.profileReader = new UserProfileReader(
                userRepositoryPort, companyRepositoryPort, usersCompanyRepositoryPort);
    }

    // Consulta um usuário e transforma o domínio no formato devolvido pela API.
    public UserProfile execute(UUID userId) {
        return profileReader.read(profileReader.requireUser(userId));
    }
}
