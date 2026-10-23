package com.escapa.backend.infrastructure.pdf;

import com.escapa.backend.domain.certificate.CertificateTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificateAssetsTest {

    @Test
    void shouldInlineFontFilesIntoTheStylesheet() {
        final CertificateAssets assets = new CertificateAssets();
        final String stylesheet = assets.stylesheet();

        assertTrue(stylesheet.contains("data:font/woff2;base64,"));
        assertFalse(stylesheet.contains("{{"));
    }

    @Test
    void shouldUseDifferentInlineLogosForEachTheme() {
        final CertificateAssets assets = new CertificateAssets();
        final String darkLogo = assets.logo(CertificateTheme.DARK);
        final String lightLogo = assets.logo(CertificateTheme.LIGHT);

        assertTrue(darkLogo.startsWith("data:image/png;base64,"));
        assertTrue(lightLogo.startsWith("data:image/png;base64,"));
        assertNotEquals(darkLogo, lightLogo);
    }
}
