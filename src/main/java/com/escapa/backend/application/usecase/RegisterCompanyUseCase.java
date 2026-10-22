package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import com.escapa.backend.application.port.ContactValidatorPort;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.company.CnpjValidator;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserTypes;

public class RegisterCompanyUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenProviderPort tokenProviderPort;
    private final ContactValidatorPort contactValidatorPort;

    public RegisterCompanyUseCase(
            UserRepositoryPort userRepositoryPort,
            CompanyRepositoryPort companyRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            TokenProviderPort tokenProviderPort,
            ContactValidatorPort contactValidatorPort
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.companyRepositoryPort = companyRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenProviderPort = tokenProviderPort;
        this.contactValidatorPort = contactValidatorPort;
    }

    public LoginOutput execute(
            String responsibleName,
            String cnpj,
            String companyName,
            String email,
            String phone,
            String password
    ) {
        requireText(responsibleName, "responsibleName");
        requireText(companyName, "companyName");
        final String normalizedCnpj = normalizeCnpj(cnpj);
        final String normalizedEmail = normalizeEmail(email);
        final String normalizedPhone = contactValidatorPort.normalizePhone(phone);
        requireValidPassword(password);

        if (userRepositoryPort.existsByEmail(normalizedEmail)) {
            throw new DuplicateRegistrationException("email");
        }
        if (companyRepositoryPort.existsByCnpjId(normalizedCnpj)) {
            throw new DuplicateRegistrationException("cnpj");
        }

        final UUID id = UUID.randomUUID();
        final User user = new User(id, responsibleName.trim(), normalizedEmail,
                passwordHasherPort.hash(password), UserTypes.COMPANY, null, LocalDateTime.now());
        final Company company = new Company(id, companyName.trim(), normalizedCnpj, normalizedEmail,
                normalizedPhone);
        companyRepositoryPort.save(user, company);

        final String token = tokenProviderPort.generateToken(
                id, normalizedEmail, UserTypes.COMPANY, UserTypes.COMPANY);
        return new LoginOutput(token, "Bearer", tokenProviderPort.getExpirationSeconds(), user, UserTypes.COMPANY);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static String normalizeCnpj(String cnpj) {
        final String normalized = CnpjValidator.normalize(cnpj);
        if (!CnpjValidator.isValid(normalized)) {
            throw new IllegalArgumentException("cnpj is invalid");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        final String normalized = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        if (!contactValidatorPort.isValidEmail(normalized)) {
            throw new IllegalArgumentException("Invalid email");
        }
        return normalized;
    }

    private static void requireValidPassword(String password) {
        if (password == null || password.length() < CreateUserUseCase.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "password must have at least " + CreateUserUseCase.MIN_PASSWORD_LENGTH + " characters");
        }
    }
}
