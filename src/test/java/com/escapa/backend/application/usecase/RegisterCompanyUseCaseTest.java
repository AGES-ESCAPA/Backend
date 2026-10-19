package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.company.DuplicateRegistrationException;
import com.escapa.backend.domain.entity.Company;

class RegisterCompanyUseCaseTest {

    private static final String RESPONSIBLE = "Maria Souza";
    private static final String COMPANY_NAME = "Hotel Vista Mar LTDA";
    private static final String CNPJ = "11222333000181";
    private static final String OTHER_CNPJ = "11444777000161";
    private static final String EMAIL = "rh@vistamar.com.br";
    private static final String PHONE = "51999990001";
    private static final String PASSWORD = "senha1234";

    private InMemoryUserRepositoryPort userRepository;
    private InMemoryCompanyRepositoryPort companyRepository;
    private FakePasswordHasher passwordHasher;
    private RegisterCompanyUseCase useCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepositoryPort();
        companyRepository = new InMemoryCompanyRepositoryPort(userRepository);
        passwordHasher = new FakePasswordHasher();
        useCase = new RegisterCompanyUseCase(
                userRepository, companyRepository, passwordHasher, new FakeTokenProviderPort());
    }

    private LoginOutput register(String cnpj, String email) {
        return useCase.execute(RESPONSIBLE, cnpj, COMPANY_NAME, email, PHONE, PASSWORD);
    }

    @Test
    void shouldRegisterCompanyAndReturnTokenWithCompanyProfile() {
        final LoginOutput output = register(CNPJ, EMAIL);

        assertNotNull(output.accessToken());
        assertEquals("Bearer", output.tokenType());
        assertEquals(3600L, output.expiresIn());
        assertEquals("COMPANY", output.profile());
        assertEquals(RESPONSIBLE, output.user().getName());
        assertEquals(EMAIL, output.user().getEmail());
        assertEquals("COMPANY", output.user().getUserType());
    }

    @Test
    void shouldPersistUserAndCompanyWithSameId() {
        final LoginOutput output = register(CNPJ, EMAIL);

        final Company company = companyRepository.find(output.user().getId());
        assertNotNull(company);
        assertEquals(COMPANY_NAME, company.getCompanyName());
        assertEquals(CNPJ, company.getCnpjId());
        assertEquals(EMAIL, company.getCompanyEmail());
        assertEquals(PHONE, company.getPhone());
        assertTrue(userRepository.findById(output.user().getId()).isPresent());
        assertNotEquals(PASSWORD, output.user().getPasswordHash());
    }

    @Test
    void shouldStoreOnlyDigitsOfCnpjAndPhoneAndNormalizeEmail() {
        final LoginOutput output = useCase.execute(
                RESPONSIBLE, "11.222.333/0001-81", COMPANY_NAME, "  RH@VistaMar.com.br ",
                "(51) 99999-0001", PASSWORD);

        final Company company = companyRepository.find(output.user().getId());
        assertEquals(CNPJ, company.getCnpjId());
        assertEquals(PHONE, company.getPhone());
        assertEquals(EMAIL, company.getCompanyEmail());
        assertEquals(EMAIL, output.user().getEmail());
    }

    @Test
    void shouldAllowRegistrationWithoutPhone() {
        final LoginOutput output = useCase.execute(RESPONSIBLE, CNPJ, COMPANY_NAME, EMAIL, null, PASSWORD);

        assertNull(companyRepository.find(output.user().getId()).getPhone());
    }

    @Test
    void shouldRejectBlankResponsibleName() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("  ", CNPJ, COMPANY_NAME, EMAIL, PHONE, PASSWORD));

        assertEquals("responsibleName is required", exception.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(null, CNPJ, COMPANY_NAME, EMAIL, PHONE, PASSWORD));
    }

    @Test
    void shouldRejectBlankCompanyName() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(RESPONSIBLE, CNPJ, "", EMAIL, PHONE, PASSWORD));

        assertEquals("companyName is required", exception.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(RESPONSIBLE, CNPJ, null, EMAIL, PHONE, PASSWORD));
    }

    @Test
    void shouldRejectInvalidCnpj() {
        final String[] invalidCnpjs = {"11222333000182", "11222333", "11111111111111", "abc", "", null};

        for (final String invalid : invalidCnpjs) {
            final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> register(invalid, EMAIL));
            assertEquals("cnpj is invalid", exception.getMessage());
        }
        assertEquals(0, companyRepository.count());
    }

    @Test
    void shouldRejectInvalidEmail() {
        final String[] invalidEmails = {"sem-arroba.com", "a@b", "@dominio.com", "", null};

        for (final String invalid : invalidEmails) {
            final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> register(CNPJ, invalid));
            assertEquals("email is invalid", exception.getMessage());
        }
        assertEquals(0, companyRepository.count());
    }

    @Test
    void shouldRejectShortPassword() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(RESPONSIBLE, CNPJ, COMPANY_NAME, EMAIL, PHONE, "1234567"));

        assertEquals("password must have at least 8 characters", exception.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(RESPONSIBLE, CNPJ, COMPANY_NAME, EMAIL, PHONE, null));
    }

    @Test
    void shouldRejectInvalidPhone() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(RESPONSIBLE, CNPJ, COMPANY_NAME, EMAIL, "12345", PASSWORD));

        assertEquals("phone is invalid", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateEmailNamingTheField() {
        register(CNPJ, EMAIL);

        final DuplicateRegistrationException exception = assertThrows(DuplicateRegistrationException.class,
                () -> register(OTHER_CNPJ, EMAIL));

        assertEquals("email", exception.getField());
        assertEquals("email already registered", exception.getMessage());
        assertEquals(1, companyRepository.count());
    }

    @Test
    void shouldRejectDuplicateCnpjNamingTheField() {
        register(CNPJ, EMAIL);

        final DuplicateRegistrationException exception = assertThrows(DuplicateRegistrationException.class,
                () -> register(CNPJ, "outro@vistamar.com.br"));

        assertEquals("cnpj", exception.getField());
        assertEquals("cnpj already registered", exception.getMessage());
        assertEquals(1, companyRepository.count());
    }
}