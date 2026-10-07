package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InactiveUserException;
import com.escapa.backend.domain.user.InvalidCredentialsException;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginUseCaseTest {

    private InMemoryUserRepositoryPort repository;
    private FakePasswordHasher passwordHasher;
    private FakeTokenProviderPort tokenProvider;
    private LoginUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepositoryPort();
        passwordHasher = new FakePasswordHasher();
        tokenProvider = new FakeTokenProviderPort();
        useCase = new LoginUseCase(repository, passwordHasher, tokenProvider);
    }

    @Test
    void shouldAuthenticateSuccessfullyAndReturnStudentProfile() {
        final UUID userId = UUID.randomUUID();
        final User user = new User(
                userId,
                "Mariana Costa",
                "mariana.costa@email.com",
                passwordHasher.hash("password123"),
                "STUDENT",
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
        repository.save(user);

        final LoginOutput output = useCase.execute("mariana.costa@email.com", "password123");

        assertNotNull(output);
        assertEquals("Bearer", output.tokenType());
        assertEquals(3600L, output.expiresIn());
        assertEquals("mocked-jwt-student", output.accessToken());
        assertEquals("STUDENT", output.profile());
        assertEquals(userId, output.user().getId());
        assertEquals("mariana.costa@email.com", output.user().getEmail());
    }

    @Test
    void shouldAuthenticateSuccessfullyAndReturnEmployeeProfile() {
        final UUID userId = UUID.randomUUID();
        final User user = new User(
                userId,
                "Luciana Prado",
                "luciana.prado@vistamar.com.br",
                passwordHasher.hash("escapa@2026"),
                "STUDENT",
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
        repository.save(user);
        repository.linkUserToCompany(userId);

        final LoginOutput output = useCase.execute("luciana.prado@vistamar.com.br", "escapa@2026");

        assertNotNull(output);
        assertEquals("EMPLOYEE", output.profile());
        assertEquals("mocked-jwt-employee", output.accessToken());
    }

    @Test
    void shouldAuthenticateSuccessfullyForAdminAndCompany() {
        final User admin = new User(
                UUID.randomUUID(),
                "Barbara Admin",
                "admin@escapa.com",
                passwordHasher.hash("admin123"),
                "ADMIN",
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
        final User company = new User(
                UUID.randomUUID(),
                "Hotel Empresa",
                "contato@hotel.com",
                passwordHasher.hash("hotel123"),
                "COMPANY",
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
        repository.save(admin);
        repository.save(company);

        final LoginOutput adminOutput = useCase.execute("admin@escapa.com", "admin123");
        final LoginOutput companyOutput = useCase.execute("contato@hotel.com", "hotel123");

        assertEquals("ADMIN", adminOutput.profile());
        assertEquals("COMPANY", companyOutput.profile());
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenEmailDoesNotExist() {
        final InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> useCase.execute("inexistente@email.com", "senha123")
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void shouldThrowInvalidCredentialsExceptionWhenPasswordIsIncorrectWithIdenticalMessage() {
        final User user = new User(
                UUID.randomUUID(),
                "Mariana Costa",
                "mariana.costa@email.com",
                passwordHasher.hash("senha-correta"),
                "STUDENT",
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
        repository.save(user);

        final InvalidCredentialsException wrongPasswordException = assertThrows(
                InvalidCredentialsException.class,
                () -> useCase.execute("mariana.costa@email.com", "senha-errada")
        );

        final InvalidCredentialsException nonExistentEmailException = assertThrows(
                InvalidCredentialsException.class,
                () -> useCase.execute("inexistente@email.com", "senha-errada")
        );

        assertEquals("Invalid email or password", wrongPasswordException.getMessage());
        assertEquals(nonExistentEmailException.getMessage(), wrongPasswordException.getMessage());
    }

    @Test
    void shouldThrowInactiveUserExceptionWhenUserIsInactive() {
        final User inactiveUser = new User(
                UUID.randomUUID(),
                "Carla Menezes",
                "carla.menezes@email.com",
                passwordHasher.hash("escapa@2026"),
                "STUDENT",
                UserStatus.INACTIVE,
                LocalDateTime.now()
        );
        repository.save(inactiveUser);

        final InactiveUserException exception = assertThrows(
                InactiveUserException.class,
                () -> useCase.execute("carla.menezes@email.com", "escapa@2026")
        );

        assertEquals("User account is inactive", exception.getMessage());
    }

    @Test
    void shouldNormalizeEmailBeforeAuthenticating() {
        final User user = new User(
                UUID.randomUUID(),
                "Mariana Costa",
                "mariana.costa@email.com",
                passwordHasher.hash("password123"),
                "STUDENT",
                UserStatus.ACTIVE,
                LocalDateTime.now()
        );
        repository.save(user);

        final LoginOutput output = useCase.execute("  MARIANA.COSTA@EMAIL.COM  ", "password123");

        assertNotNull(output);
        assertEquals("mariana.costa@email.com", output.user().getEmail());
    }

    @Test
    void shouldRejectBlankOrNullCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute(null, "senha"));
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("   ", "senha"));
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("user@email.com", null));
        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("user@email.com", "   "));
    }
}

