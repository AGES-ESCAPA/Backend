package com.escapa.backend.domain.content;

import com.escapa.backend.domain.entity.Content;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentTypeRulesTest {

    @Test
    void shouldRejectMissingType() {
        final Content content = new Content();

        final IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(content));

        assertEquals("Type is required", exception.getMessage());
    }

    @Test
    void shouldRequireVideoUrlAndPositiveDuration() {
        final Content missingUrl = content(ContentType.VIDEO, null, null, null);
        assertEquals("url is required for content type VIDEO",
                assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(missingUrl))
                        .getMessage());

        final Content missingDuration = content(ContentType.VIDEO, "https://example.com/video", null, null);
        assertEquals("durationMinutes is required for content type VIDEO",
                assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(missingDuration))
                        .getMessage());

        final Content zeroDuration = content(ContentType.VIDEO, "https://example.com/video", 0, null);
        assertEquals("durationMinutes must be greater than zero",
                assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(zeroDuration))
                        .getMessage());

        final Content validVideo = content(ContentType.VIDEO, "https://example.com/video", 1, null);
        assertDoesNotThrow(() -> ContentTypeRules.validate(validVideo));
    }

    @Test
    void shouldRequireFileUrlAndTextDescription() {
        final Content missingFileUrl = content(ContentType.FILE, "  ", null, null);
        assertEquals("url is required for content type FILE",
                assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(missingFileUrl))
                        .getMessage());

        final Content validFile = content(ContentType.FILE, "https://example.com/file.pdf", null, null);
        assertDoesNotThrow(() -> ContentTypeRules.validate(validFile));

        final Content missingText = content(ContentType.TEXT, null, null, "\t ");
        assertEquals("description is required for content type TEXT",
                assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(missingText))
                        .getMessage());

        final Content validText = content(ContentType.TEXT, null, null, "Lesson text");
        assertDoesNotThrow(() -> ContentTypeRules.validate(validText));
    }

    private static Content content(ContentType type, String url, Integer duration, String description) {
        final Content content = new Content();
        content.setType(type);
        content.setUrl(url);
        content.setDurationMinutes(duration);
        content.setDescription(description);
        return content;
    }
}
