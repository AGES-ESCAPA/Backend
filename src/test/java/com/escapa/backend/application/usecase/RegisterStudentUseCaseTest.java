package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.RegistrationConflictException;

class RegisterStudentUseCaseTest {

    private InMemoryUserRepositoryPort userRepository;
    private InMemoryRegularUserRepositoryPort regularUserRepository;
    private FakeTokenProviderPort tokenProvider;
    private RegisterStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepositoryPort();
        regularUserRepository = new InMemoryRegularUserRepositoryPort();
        tokenProvider = new FakeTokenProviderPort();
        useCase = new RegisterStudentUseCase(
                userRepository, regularUserRepository, new FakePasswordHasher(), tokenProvider);
    }

    @Test
    void shouldRegisterStudentWithNormalizedDataAndToken() {
        final LoginOutput output = useCase.execute(command(
                "  Maria da Silva  ", "529.982.247-25", "  MARIA@EMAIL.COM  ", "(51) 99999-0000", "12345678"));

        final User saved = regularUserRepository.findByCpf("52998224725");
        assertNotNull(saved);
        assertEquals("Maria da Silva", saved.getName());
        assertEquals("maria@email.com", saved.getEmail());
        assertEquals("hashed:12345678", saved.getPasswordHash());
        assertEquals("STUDENT", saved.getUserType());
        assertEquals("51999990000", regularUserRepository.findPhoneByUserId(saved.getId()));
        assertEquals("mocked-jwt-student", output.accessToken());
        assertEquals("Bearer", output.tokenType());
        assertEquals(3600L, output.expiresIn());
        assertEquals("STUDENT", output.profile());
        assertEquals(saved.getId(), tokenProvider.getLastUserId());
        assertEquals("STUDENT", tokenProvider.getLastRole());
        assertEquals("STUDENT", tokenProvider.getLastProfile());
    }

    @Test
    void shouldRejectMissingName() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command(" ", "52998224725", "maria@email.com", null, "12345678")));

        assertEquals("Name is required", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidCpf() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command("Maria", "111.111.111-11", "maria@email.com", null, "12345678")));

        assertEquals("Invalid CPF", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidEmail() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command("Maria", "52998224725", "email-invalido", null, "12345678")));

        assertEquals("Invalid email", exception.getMessage());
    }

    @Test
    void shouldRejectShortPassword() {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(command("Maria", "52998224725", "maria@email.com", null, "1234567")));

        assertEquals("Password must be at least 8 characters", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateEmailAndIdentifyField() {
        userRepository.save(new User("Outra Maria", "maria@email.com", "hash", "STUDENT"));

        final RegistrationConflictException exception = assertThrows(
                RegistrationConflictException.class,
                () -> useCase.execute(command("Maria", "52998224725", "maria@email.com", null, "12345678")));

        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateCpfAndIdentifyField() {
        regularUserRepository.save(
                new User("Outra Maria", "outra@email.com", "hash", "STUDENT"), "52998224725", null);

        final RegistrationConflictException exception = assertThrows(
                RegistrationConflictException.class,
                () -> useCase.execute(command("Maria", "529.982.247-25", "maria@email.com", null, "12345678")));

        assertEquals("CPF already registered", exception.getMessage());
    }

    private static RegisterStudentCommand command(
            String name, String cpf, String email, String phone, String password
    ) {
        return new RegisterStudentCommand(name, cpf, email, phone, password);
    }
}
