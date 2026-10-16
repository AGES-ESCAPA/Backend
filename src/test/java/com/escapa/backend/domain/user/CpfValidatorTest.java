package com.escapa.backend.domain.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CpfValidatorTest {

    @Test
    void shouldAcceptFormattedAndUnformattedValidCpf() {
        assertTrue(CpfValidator.isValid("529.982.247-25"));
        assertTrue(CpfValidator.isValid("52998224725"));
    }

    @Test
    void shouldRejectWrongCheckDigitsRepeatedDigitsAndLetters() {
        assertFalse(CpfValidator.isValid("529.982.247-24"));
        assertFalse(CpfValidator.isValid("111.111.111-11"));
        assertFalse(CpfValidator.isValid("abc52998224725"));
    }
}
