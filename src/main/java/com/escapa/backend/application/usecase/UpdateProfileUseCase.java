package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.port.UsersCompanyRepositoryPort;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.ProfileNotEditableException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserTypes;

public class UpdateProfileUseCase {

    private static final int PHONE_MIN_DIGITS = 10;
    private static final int PHONE_MAX_DIGITS = 11;

    // Porta que permite acessar os usuários sem acoplar o serviço ao JPA.
    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;
    private final UserProfileReader profileReader;

    // Recebe a implementação da porta fornecida pela infraestrutura.
    public UpdateProfileUseCase(UserRepositoryPort userRepositoryPort,
                                CompanyRepositoryPort companyRepositoryPort,
                                UsersCompanyRepositoryPort usersCompanyRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.companyRepositoryPort = companyRepositoryPort;
        this.profileReader = new UserProfileReader(
                userRepositoryPort, companyRepositoryPort, usersCompanyRepositoryPort);
    }

    // Atualiza os dados básicos do perfil.
    public UserProfile execute(UUID userId, String name, String phone, String companyName) {
        final User user = profileReader.requireUser(userId);
        final String profile = profileReader.resolveProfile(user);

        // Usuários do tipo EMPLOYEE podem visualizar, mas não editar o perfil.
        if (!UserProfileReader.isEditable(profile)) {
            throw new ProfileNotEditableException(userId);
        }

        final String normalizedName = requireText(name, "name");
        final String normalizedPhone = normalizePhone(phone);
        final String normalizedCompanyName = UserTypes.COMPANY.equals(profile)
                ? requireText(companyName, "companyName")
                : null;

        // Copia os dados recebidos para a entidade de domínio antes de salvar.
        user.setName(normalizedName);

        // Persiste o usuário e usa o objeto salvo para montar a resposta final.
        userRepositoryPort.save(user);
        if (UserTypes.COMPANY.equals(profile)) {
            updateCompany(userId, normalizedCompanyName, normalizedPhone);
        } else {
            userRepositoryPort.updateStudentPhone(userId, normalizedPhone);
        }
        return profileReader.read(profileReader.requireUser(userId));
    }

    private void updateCompany(UUID companyId, String companyName, String phone) {
        final Company company = companyRepositoryPort.findById(companyId)
                .orElseThrow(() -> new UserNotFoundException(companyId));
        company.setCompanyName(companyName);
        company.setPhone(phone);
        companyRepositoryPort.update(company);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        final String digits = phone.replaceAll("\\D", "");
        if (digits.length() < PHONE_MIN_DIGITS || digits.length() > PHONE_MAX_DIGITS) {
            throw new IllegalArgumentException("phone is invalid");
        }
        return digits;
    }
}
