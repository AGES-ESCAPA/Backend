package com.escapa.backend.domain.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CertificateThemeTest {

    @Test
    void shouldParseLightAndDarkIgnoringCase() {
        assertEquals(CertificateTheme.LIGHT, CertificateTheme.fromParam("light"));
        assertEquals(CertificateTheme.DARK, CertificateTheme.fromParam("DARK"));
        assertEquals(CertificateTheme.DARK, CertificateTheme.fromParam(" Dark "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"blue", "escuro", "lightdark"})
    void shouldRejectAnyOtherValue(String value) {
        assertThrows(IllegalArgumentException.class, () -> CertificateTheme.fromParam(value));
    }

    @Test
    void shouldExposeTheLowercaseUrlValue() {
        assertEquals("light", CertificateTheme.LIGHT.param());
        assertEquals("dark", CertificateTheme.DARK.param());
    }
}
