package com.escapa.backend.infrastructure.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.certificate.CertificateTheme;

class CertificateAssetsTest {

    private final CertificateAssets assets = new CertificateAssets();

    @Test
    void shouldLoadStylesheetWithInlinedFontAndLayoutAssets() {
        final String stylesheet = assets.stylesheet();

        assertFalse(stylesheet.isBlank());
        assertTrue(stylesheet.contains("data:font/woff2;base64,") || stylesheet.contains("@font-face"));
        assertTrue(stylesheet.contains("data:") || stylesheet.contains("{"));
    }

    @Test
    void shouldExposeThemeLogosAsDataUris() {
        assertTrue(assets.logo(CertificateTheme.DARK).startsWith("data:image/png;base64,"));
        assertTrue(assets.logo(CertificateTheme.LIGHT).startsWith("data:image/png;base64,"));
    }

    @Test
    void shouldResolvePngMediaTypeAndRejectUnsupportedAssets() throws Exception {
        final Method mediaType = CertificateAssets.class.getDeclaredMethod("mediaType", String.class);
        mediaType.setAccessible(true);

        assertEquals("image/png", mediaType.invoke(null, "images/escapa-logo.png"));
        assertEquals("font/woff2", mediaType.invoke(null, "fonts/inter-latin.woff2"));

        final InvocationTargetException error = assertThrows(
                InvocationTargetException.class,
                () -> mediaType.invoke(null, "images/logo.svg"));
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
        assertEquals("Unsupported certificate asset: images/logo.svg", error.getCause().getMessage());
    }

    @Test
    void shouldFailWhenClasspathAssetIsMissing() throws Exception {
        final Method bytes = CertificateAssets.class.getDeclaredMethod("bytes", String.class);
        bytes.setAccessible(true);

        final InvocationTargetException error = assertThrows(
                InvocationTargetException.class,
                () -> bytes.invoke(null, "missing-asset.bin"));
        assertInstanceOf(UncheckedIOException.class, error.getCause());
        assertTrue(error.getCause().getMessage().contains("Missing certificate asset: certificate/missing-asset.bin"));
    }
}
