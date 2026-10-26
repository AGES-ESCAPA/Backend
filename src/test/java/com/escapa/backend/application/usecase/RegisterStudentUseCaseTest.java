package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.infrastructure.validation.RegistrationContactValidator;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class RegisterStudentUseCaseTest {

    private static ValidatorFactory validatorFactory;
    private InMemoryUserRepositoryPort userRepository;
    private InMemoryRegularUserRepositoryPort regularUserRepository;
    private FakeTokenProviderPort tokenProvider;
    private RegisterStudentUseCase useCase;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepositoryPort();
        regularUserRepository = new InMemoryRegularUserRepositoryPort(userRepository);
        tokenProvider = new FakeTokenProviderPort();
        useCase = new RegisterStudentUseCase(
                userRepository, regularUserRepository, new FakePasswordHasher(), tokenProvider,
                new RegistrationContactValidator(validatorFactory.getValidator()));
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
        assertEquals(saved.getId(), userRepository.findByEmail("maria@email.com").orElseThrow().getId());
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

        final DuplicateRegistrationException exception = assertThrows(
                DuplicateRegistrationException.class,
                () -> useCase.execute(command("Maria", "52998224725", "maria@email.com", null, "12345678")));

        assertEquals("email already registered", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateCpfAndIdentifyField() {
        regularUserRepository.save(
                new User("Outra Maria", "outra@email.com", "hash", "STUDENT"), "52998224725", null);

        final DuplicateRegistrationException exception = assertThrows(
                DuplicateRegistrationException.class,
                () -> useCase.execute(command("Maria", "529.982.247-25", "maria@email.com", null, "12345678")));

        assertEquals("cpf already registered", exception.getMessage());
    }

    @Test
    void shouldAllowLoginAfterRegistrationWithoutChangingPasswordWhitespace() {
        final String password = "  senha-segura  ";
        final LoginOutput registered = useCase.execute(command(
                "Maria", "52998224725", "MARIA@EMAIL.COM", null, password));
        final LoginUseCase login = new LoginUseCase(userRepository, new FakePasswordHasher(), tokenProvider);

        final LoginOutput authenticated = login.execute("maria@email.com", password);

        assertEquals(registered.user().getId(), authenticated.user().getId());
        assertEquals("STUDENT", authenticated.profile());
    }

    @Test
    void shouldRejectASecondRegistrationWithTheSameEmailAndAnotherCpf() {
        useCase.execute(command("Maria", "52998224725", "maria@email.com", null, "12345678"));

        final DuplicateRegistrationException exception = assertThrows(DuplicateRegistrationException.class,
                () -> useCase.execute(command("Outra", "12345678909", " MARIA@EMAIL.COM ", null, "12345678")));

        assertEquals("email already registered", exception.getMessage());
        assertEquals(1, userRepository.findAll().size());
        assertNull(regularUserRepository.findByCpf("12345678909"));
    }

    @Test
    void shouldRejectASecondRegistrationWithTheSameCpfAndAnotherEmail() {
        useCase.execute(command("Maria", "52998224725", "maria@email.com", null, "12345678"));

        final DuplicateRegistrationException exception = assertThrows(DuplicateRegistrationException.class,
                () -> useCase.execute(command("Outra", "529.982.247-25", "outra@email.com", null, "12345678")));

        assertEquals("cpf already registered", exception.getMessage());
        assertTrue(userRepository.findByEmail("outra@email.com").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldTreatAnAbsentPhoneAsNull(String phone) {
        final LoginOutput output = useCase.execute(command("Maria", "52998224725", "maria@email.com", phone, "12345678"));

        assertNull(regularUserRepository.findPhoneByUserId(output.user().getId()));
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void shouldRejectInvalidFieldsBeforeSavingOrGeneratingAToken(RegisterStudentCommand input, String message) {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(input));

        assertEquals(message, exception.getMessage());
        assertTrue(userRepository.findAll().isEmpty());
        assertNull(tokenProvider.getLastUserId());
    }

    private static Stream<Arguments> invalidCommands() {
        final String longText = "x".repeat(RegisterStudentUseCase.MAX_FIELD_LENGTH + 1);
        return Stream.of(
                Arguments.of(null, "Name is required"),
                invalid(null, "52998224725", "maria@email.com", null, "12345678", "Name is required"),
                invalid("Maria", null, "maria@email.com", null, "12345678", "CPF is required"),
                invalid("Maria", " ", "maria@email.com", null, "12345678", "CPF is required"),
                invalid("Maria", "52998224725", null, null, "12345678", "Email is required"),
                invalid("Maria", "52998224725", " ", null, "12345678", "Email is required"),
                invalid("Maria", "52998224725", "a..b@example.com", null, "12345678", "Invalid email"),
                invalid("Maria", "52998224725", "a@-example.com", null, "12345678", "Invalid email"),
                invalid("Maria", "52998224725", "maria@email.com", null, null, "Password is required"),
                invalid("Maria", "52998224725", "maria@email.com", null, "        ", "Password is required"),
                invalid("Maria", "52998224725", "maria@email.com", "abc", "12345678", "Invalid phone"),
                invalid("Maria", "52998224725", "maria@email.com", "+1 123", "12345678", "Invalid phone"),
                invalid("Maria", "52998224725", "maria@email.com", "+55 (51) 99999-0000", "12345678", "Invalid phone"),
                invalid(longText, "52998224725", "maria@email.com", null, "12345678", "name must have at most 255 characters"),
                invalid("Maria", longText, "maria@email.com", null, "12345678", "cpf must have at most 255 characters"),
                invalid("Maria", "52998224725", longText, null, "12345678", "email must have at most 255 characters"),
                invalid("Maria", "52998224725", "maria@email.com", longText, "12345678", "phone must have at most 255 characters")
        );
    }

    private static Arguments invalid(String name, String cpf, String email, String phone, String password, String message) {
        return Arguments.of(command(name, cpf, email, phone, password), message);
    }

    private static RegisterStudentCommand command(
            String name, String cpf, String email, String phone, String password
    ) {
        return new RegisterStudentCommand(name, cpf, email, phone, password);
    }
}
