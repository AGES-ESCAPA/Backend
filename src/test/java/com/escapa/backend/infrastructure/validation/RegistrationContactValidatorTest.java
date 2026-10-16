package com.escapa.backend.infrastructure.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.escapa.backend.adapters.dto.RegisterStudentRequest;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

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
    @CsvSource({
            "(54) 99999-8888, +5554999998888",
            "+55 (51) 3333-4444, +555133334444",
            "+1 (650) 253-0000, +16502530000",
            "＋１（６５０）２５３－００００, +16502530000",
            "+١ ٦٥٠ ٢٥٣ ٠٠٠٠, +16502530000",
            "+44 20 7946 0018, +442079460018",
            "0044 20 7946 0018, +442079460018",
            "+33 1 42 34 56 78, +33142345678",
            "+81 3-1234-5678, +81312345678",
            "+61 2 9374 4000, +61293744000",
            "+91 98765 43210, +919876543210",
            "+27 11 123 4567, +27111234567",
            "+800 1234 5678, +80012345678",
            "+1 (650) 253-0000 ext. 123, +16502530000;ext=123",
            "+55 (51) 3333-4444 ramal 123, +555133334444;ext=123",
            "+442079460018;ext=123, +442079460018;ext=123"
    })
    void shouldNormalizeInternationalFormatsAndPreserveExtensions(String phone, String expected) {
        assertEquals(expected, contacts.normalizePhone(phone));
        assertEquals(expected, contacts.normalizePhone(expected));
    }

    @Test
    void shouldAcceptAllNumberTypesSupportedByTheLibrary() {
        final PhoneNumberUtil numbers = PhoneNumberUtil.getInstance();
        for (final PhoneNumberType type : PhoneNumberType.values()) {
            if (type == PhoneNumberType.UNKNOWN) {
                continue;
            }
            final PhoneNumber example = numbers.getExampleNumberForType(type);
            if (example != null) {
                final String international = numbers.format(example, PhoneNumberFormat.E164);
                assertEquals(international, contacts.normalizePhone(international), type.name());
            }
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void shouldTreatAnAbsentPhoneAsNull(String phone) {
        assertNull(contacts.normalizePhone(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "()--", "123", "+999 123456789", "+1 123", "abc+16502530000", "+16502530000 lixo"})
    void shouldRejectInvalidPhonesInsteadOfSilentlyRemovingLetters(String phone) {
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
    @ValueSource(strings = {"maria@example.com", "maria.silva+curso@example.com", "  MARIA@EXAMPLE.COM  "})
    void shouldAcceptValidEmailsWithTheSameRuleAsTheRequest(String email) {
        final RegisterStudentRequest request = new RegisterStudentRequest("Maria", "52998224725", email, null, "12345678");
        assertTrue(contacts.isValidEmail(request.email()));
        assertTrue(validatorFactory.getValidator().validate(request).isEmpty());
    }
}
