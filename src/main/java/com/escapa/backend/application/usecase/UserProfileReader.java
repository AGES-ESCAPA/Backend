package com.escapa.backend.application.usecase;

import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.model.StudentContact;
import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.port.UsersCompanyRepositoryPort;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserTypes;

final class UserProfileReader {

    static final String EMPLOYEE = "EMPLOYEE";

    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;
    private final UsersCompanyRepositoryPort usersCompanyRepositoryPort;

    UserProfileReader(UserRepositoryPort userRepositoryPort,
                      CompanyRepositoryPort companyRepositoryPort,
                      UsersCompanyRepositoryPort usersCompanyRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.companyRepositoryPort = companyRepositoryPort;
        this.usersCompanyRepositoryPort = usersCompanyRepositoryPort;
    }

    // Centraliza a busca e mantém o erro de usuário inexistente padronizado.
    User requireUser(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    String resolveProfile(User user) {
        if (UserTypes.STUDENT.equals(user.getUserType())
                && usersCompanyRepositoryPort.existsByUserId(user.getId())) {
            return EMPLOYEE;
        }
        return user.getUserType();
    }

    // STUDENT e COMPANY podem editar o perfil; EMPLOYEE pode apenas consultá-lo.
    static boolean isEditable(String profile) {
        return UserTypes.STUDENT.equals(profile) || UserTypes.COMPANY.equals(profile);
    }

    // Converte a entidade de domínio para o formato exposto pela API.
    UserProfile read(User user) {
        final String profile = resolveProfile(user);
        if (UserTypes.COMPANY.equals(profile)) {
            return companyProfile(user);
        }
        if (UserTypes.STUDENT.equals(profile) || EMPLOYEE.equals(profile)) {
            return studentProfile(user, profile);
        }
        return new UserProfile(profile, false, user.getName(), user.getEmail(), null, null, null, null,
                user.getAvatarUrl());
    }

    private UserProfile companyProfile(User user) {
        final Optional<Company> company = companyRepositoryPort.findById(user.getId());
        return new UserProfile(
                UserTypes.COMPANY,
                true,
                user.getName(),
                user.getEmail(),
                null,
                company.map(Company::getPhone).orElse(null),
                company.map(Company::getCompanyName).orElse(null),
                company.map(Company::getCnpjId).orElse(null),
                user.getAvatarUrl()
        );
    }

    private UserProfile studentProfile(User user, String profile) {
        final Optional<StudentContact> contact = userRepositoryPort.findStudentContact(user.getId());
        final String companyName = EMPLOYEE.equals(profile) ? employerName(user.getId()) : null;
        return new UserProfile(
                profile,
                isEditable(profile),
                user.getName(),
                user.getEmail(),
                contact.map(StudentContact::cpf).orElse(null),
                contact.map(StudentContact::phone).orElse(null),
                companyName,
                null,
                user.getAvatarUrl()
        );
    }

    private String employerName(UUID userId) {
        return usersCompanyRepositoryPort.findCompanyIdByUserId(userId)
                .flatMap(companyRepositoryPort::findById)
                .map(Company::getCompanyName)
                .orElse(null);
    }
}
