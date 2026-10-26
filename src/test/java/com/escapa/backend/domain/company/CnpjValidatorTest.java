package com.escapa.backend.domain.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CnpjValidatorTest {

    @Test
    void shouldAcceptValidNumericCnpjs() {
        assertTrue(CnpjValidator.isValid("11222333000181"));
        assertTrue(CnpjValidator.isValid("11444777000161"));
    }

    @Test
    void shouldAcceptValidAlphanumericCnpj() {
        assertTrue(CnpjValidator.isValid("12ABC34501DE35"));
    }

    @Test
    void shouldRejectWrongCheckDigits() {
        assertFalse(CnpjValidator.isValid("11222333000182"));
        assertFalse(CnpjValidator.isValid("12ABC34501DE36"));
        assertFalse(CnpjValidator.isValid("12ABC34501DE45"));
    }

    @Test
    void shouldRejectLettersInCheckDigitPositions() {
        assertFalse(CnpjValidator.isValid("12ABC34501DEA5"));
    }

    @Test
    void shouldRejectWrongLengthAndRepeatedCharacters() {
        assertFalse(CnpjValidator.isValid("12ABC34501DE3"));
        assertFalse(CnpjValidator.isValid("12ABC34501DE355"));
        assertFalse(CnpjValidator.isValid("11111111111111"));
        assertFalse(CnpjValidator.isValid("00000000000000"));
    }

    @Test
    void shouldNotAcceptLowercaseOrNullWithoutNormalizing() {
        assertFalse(CnpjValidator.isValid("12abc34501de35"));
        assertFalse(CnpjValidator.isValid(null));
        assertFalse(CnpjValidator.isValid(""));
    }

    @Test
    void shouldNormalizePunctuationAndCase() {
        assertEquals("12ABC34501DE35", CnpjValidator.normalize("12.abc.345/01de-35"));
        assertEquals("11222333000181", CnpjValidator.normalize("11.222.333/0001-81"));
        assertEquals("", CnpjValidator.normalize(null));
    }
}
