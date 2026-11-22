package com.escapa.backend.application.usecase;

import java.util.Objects;
import java.util.UUID;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.UserProfileChangeLogRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.port.UsersCompanyRepositoryPort;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserTypes;

/**
 * Atualização administrativa dos dados de cadastro/perfil de qualquer usuário,
 * inclusive colaboradores (que não editam o próprio perfil). Registra histórico
 * de alterações. E-mail não muda aqui — usa o fluxo de confirmação por token.
 */
public class AdminUpdateUserUseCase {

    private static final int PHONE_MIN_DIGITS = 10;
    private static final int PHONE_MAX_DIGITS = 11;

    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;
    private final UserProfileChangeLogRepositoryPort changeLogRepositoryPort;
    private final UserProfileReader profileReader;

    public AdminUpdateUserUseCase(
            UserRepositoryPort userRepositoryPort,
            CompanyRepositoryPort companyRepositoryPort,
            UsersCompanyRepositoryPort usersCompanyRepositoryPort,
            UserProfileChangeLogRepositoryPort changeLogRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.companyRepositoryPort = companyRepositoryPort;
        this.changeLogRepositoryPort = changeLogRepositoryPort;
        this.profileReader = new UserProfileReader(
                userRepositoryPort, companyRepositoryPort, usersCompanyRepositoryPort);
    }

    public UserProfile execute(UUID actorId, UUID userId, String name, String phone, String companyName) {
        final User user = profileReader.requireUser(userId);
        final String profile = profileReader.resolveProfile(user);
        final UserProfile before = profileReader.read(user);

        final String normalizedName = requireText(name, "name");
        final String normalizedPhone = needsPhone(profile) ? normalizePhone(phone) : null;
        final String normalizedCompanyName = UserTypes.COMPANY.equals(profile)
                ? requireText(companyName, "companyName")
                : null;

        user.setName(normalizedName);
        userRepositoryPort.save(user);

        if (UserTypes.COMPANY.equals(profile)) {
            updateCompany(userId, normalizedCompanyName, normalizedPhone);
        } else if (UserTypes.STUDENT.equals(profile)) {
            userRepositoryPort.updateStudentPhone(userId, normalizedPhone);
        }

        final UserProfile after = profileReader.read(profileReader.requireUser(userId));
        logIfChanged(actorId, userId, "name", before.name(), after.name());
        if (needsPhone(profile)) {
            logIfChanged(actorId, userId, "phone", before.phone(), after.phone());
        }
        if (UserTypes.COMPANY.equals(profile)) {
            logIfChanged(actorId, userId, "companyName", before.companyName(), after.companyName());
        }

        return after;
    }

    private void logIfChanged(UUID actorId, UUID userId, String field, String oldValue, String newValue) {
        final String oldNormalized = oldValue == null ? "" : oldValue;
        final String newNormalized = newValue == null ? "" : newValue;
        if (Objects.equals(oldNormalized, newNormalized)) {
            return;
        }
        changeLogRepositoryPort.append(userId, actorId, field, oldValue, newValue);
    }

    private static boolean needsPhone(String profile) {
        return UserTypes.STUDENT.equals(profile) || UserTypes.COMPANY.equals(profile);
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
