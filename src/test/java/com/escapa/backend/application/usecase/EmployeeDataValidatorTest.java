package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmployeeDataValidatorTest {

    private final EmployeeDataValidator validator = new EmployeeDataValidator(new FakeContactValidator());

    @Test
    void shouldTrimNameAndJobTitleAndNormalizeEmail() {
        assertEquals("Maria Silva", validator.requireName("  Maria Silva  "));
        assertEquals("Recepcionista", validator.requireJobTitle("  recepcionista  "));
        assertEquals("maria@empresa.com", validator.requireValidEmail("  Maria@Empresa.COM  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void shouldRejectBlankName(String name) {
        assertEquals(
                "Name is required",
                assertThrows(IllegalArgumentException.class, () -> validator.requireName(name)).getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankJobTitle(String jobTitle) {
        assertEquals(
                "Job title is required",
                assertThrows(IllegalArgumentException.class, () -> validator.requireJobTitle(jobTitle)).getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankEmail(String email) {
        assertEquals(
                "Email is required",
                assertThrows(IllegalArgumentException.class, () -> validator.requireValidEmail(email)).getMessage());
    }

    @Test
    void shouldRejectInvalidEmailFormat() {
        assertEquals(
                "Invalid email",
                assertThrows(IllegalArgumentException.class, () -> validator.requireValidEmail("sem-arroba"))
                        .getMessage());
    }

    @Test
    void shouldRejectFieldsLongerThanLimit() {
        final String tooLong = "a".repeat(RegisterStudentUseCase.MAX_FIELD_LENGTH + 1);

        assertEquals(
                "name must have at most " + RegisterStudentUseCase.MAX_FIELD_LENGTH + " characters",
                assertThrows(IllegalArgumentException.class, () -> validator.requireName(tooLong)).getMessage());
        assertEquals(
                "jobTitle must have at most " + RegisterStudentUseCase.MAX_FIELD_LENGTH + " characters",
                assertThrows(IllegalArgumentException.class, () -> validator.requireJobTitle(tooLong)).getMessage());
        assertEquals(
                "email must have at most " + RegisterStudentUseCase.MAX_FIELD_LENGTH + " characters",
                assertThrows(IllegalArgumentException.class, () -> validator.requireValidEmail(tooLong)).getMessage());
    }
}
