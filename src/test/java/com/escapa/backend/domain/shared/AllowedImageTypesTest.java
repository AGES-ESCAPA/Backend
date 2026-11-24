package com.escapa.backend.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AllowedImageTypesTest {

    @ParameterizedTest
    @CsvSource({
            "image/jpeg, jpg",
            "image/png, png",
            "image/webp, webp"
    })
    void shouldAllowKnownImageTypesAndMapExtensions(String contentType, String extension) {
        assertTrue(AllowedImageTypes.isAllowed(contentType));
        assertEquals(extension, AllowedImageTypes.extensionOf(contentType));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"image/gif", "application/pdf", "text/plain"})
    void shouldRejectUnknownOrBlankContentTypes(String contentType) {
        assertFalse(AllowedImageTypes.isAllowed(contentType));
    }

    @Test
    void shouldReturnNullExtensionForUnknownType() {
        assertNull(AllowedImageTypes.extensionOf("image/gif"));
    }
}
