package com.escapa.backend.application.usecase;

import java.util.regex.Pattern;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.RegularUserRepositoryPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.CpfValidator;
import com.escapa.backend.domain.user.RegistrationConflictException;
import com.escapa.backend.domain.user.UserTypes;

public class RegisterStudentUseCase {

    private static final String PROFILE = UserTypes.STUDENT;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepositoryPort userRepositoryPort;
    private final RegularUserRepositoryPort regularUserRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenProviderPort tokenProviderPort;

    public RegisterStudentUseCase(
            UserRepositoryPort userRepositoryPort,
            RegularUserRepositoryPort regularUserRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            TokenProviderPort tokenProviderPort
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.regularUserRepositoryPort = regularUserRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    public LoginOutput execute(RegisterStudentCommand command) {
        validateRequiredFields(command);
        final String name = command.name().trim();
        final String cpf = CpfValidator.digitsOnly(command.cpf());
        final String email = command.email().trim().toLowerCase();
        final String phone = normalizePhone(command.phone());
        validateBusinessFields(command, email);
        ensureUniqueData(email, cpf);

        final User user = new User(name, email, passwordHasherPort.hash(command.password()), UserTypes.STUDENT);
        final User savedUser = regularUserRepositoryPort.save(user, cpf, phone);
        final String token = tokenProviderPort.generateToken(
                savedUser.getId(), savedUser.getEmail(), UserTypes.STUDENT, PROFILE);
        return new LoginOutput(token, "Bearer", tokenProviderPort.getExpirationSeconds(), savedUser, PROFILE);
    }

    private static void validateRequiredFields(RegisterStudentCommand command) {
        if (command == null || command.name() == null || command.name().trim().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (command.cpf() == null || command.cpf().isBlank()) {
            throw new IllegalArgumentException("CPF is required");
        }
        if (command.email() == null || command.email().trim().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
    }

    private static void validateBusinessFields(RegisterStudentCommand command, String email) {
        if (!CpfValidator.isValid(command.cpf())) {
            throw new IllegalArgumentException("Invalid CPF");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (command.password() == null || command.password().length() < CreateUserUseCase.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + CreateUserUseCase.MIN_PASSWORD_LENGTH + " characters");
        }
    }

    private void ensureUniqueData(String email, String cpf) {
        if (userRepositoryPort.existsByEmail(email)) {
            throw new RegistrationConflictException("Email");
        }
        if (regularUserRepositoryPort.existsByCpf(cpf)) {
            throw new RegistrationConflictException("CPF");
        }
    }

    private static String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }
}
