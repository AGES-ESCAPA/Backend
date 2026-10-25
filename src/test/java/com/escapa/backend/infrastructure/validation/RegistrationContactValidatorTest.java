package com.escapa.backend.infrastructure.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.escapa.backend.adapters.dto.RegisterStudentRequest;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class RegistrationContactValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static RegistrationContactValidator contacts;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        contacts = new RegistrationContactValidator(validatorFactory.getValidator());
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"(54) 99999-8888", "51999990001", "(51) 3333-4444"})
    void shouldNormalizeBrazilianPhoneFormatsToDigitsOnly(String phone) {
        final String expected = phone.replaceAll("\\D", "");
        assertEquals(expected, contacts.normalizePhone(phone));
        assertEquals(expected, contacts.normalizePhone(expected));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldTreatAnAbsentPhoneAsNull(String phone) {
        assertNull(contacts.normalizePhone(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "()--", "123", "+55 (51) 99999-0000", "519999900001"})
    void shouldRejectPhonesOutsideTheBrazilianDigitLength(String phone) {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> contacts.normalizePhone(phone));
        assertEquals("Invalid phone", exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "a..b@example.com", ".a@example.com", "a.@example.com", "a@-example.com", "a@@example.com"})
    void shouldRejectInvalidEmailsWithTheSameRuleAsTheRequest(String email) {
        final RegisterStudentRequest request = new RegisterStudentRequest("Maria", "52998224725", email, null, "12345678");
        assertFalse(contacts.isValidEmail(request.email()));
        assertTrue(validatorFactory.getValidator().validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"maria@example.com", "aluno@example", "maria.silva+curso@example.com", "  MARIA@EXAMPLE.COM  "})
    void shouldAcceptValidEmailsWithTheSameRuleAsTheRequest(String email) {
        final RegisterStudentRequest request = new RegisterStudentRequest("Maria", "52998224725", email, null, "12345678");
        assertTrue(contacts.isValidEmail(request.email()));
        assertTrue(validatorFactory.getValidator().validate(request).isEmpty());
    }
}
