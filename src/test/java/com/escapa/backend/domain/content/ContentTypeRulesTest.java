package com.escapa.backend.domain.content;

import com.escapa.backend.domain.entity.Content;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentTypeRulesTest {

    @Test
    void shouldAcceptVideoFileAndTextWithTheirRequiredFields() {
        final Content video = content(ContentType.VIDEO, "https://example.com/video.mp4", null, 15);
        final Content file = content(ContentType.FILE, "https://example.com/file.pdf", null, null);
        final Content text = content(ContentType.TEXT, null, "Lesson text", null);
        video.setResources(List.of(new ContentResource("1", ContentResource.TYPE_LINK, "Guide",
                "https://example.com/guide")));

        assertDoesNotThrow(() -> ContentTypeRules.validate(video));
        assertDoesNotThrow(() -> ContentTypeRules.validate(file));
        assertDoesNotThrow(() -> ContentTypeRules.validate(text));
    }

    @Test
    void shouldRejectMissingTypeAndInvalidTypeSpecificFields() {
        assertEquals("Type is required", assertThrows(
                IllegalArgumentException.class, () -> ContentTypeRules.validate(new Content())).getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> ContentTypeRules.validate(content(ContentType.VIDEO, null, null, 10)));
        assertThrows(IllegalArgumentException.class,
                () -> ContentTypeRules.validate(content(ContentType.VIDEO, "video-url", null, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> ContentTypeRules.validate(content(ContentType.FILE, " ", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> ContentTypeRules.validate(content(ContentType.TEXT, null, "  ", null)));
    }

    @Test
    void shouldRejectInvalidSupplementalResources() {
        final Content content = content(ContentType.TEXT, null, "Text", null);
        content.setResources(List.of(new ContentResource("1", "other", "Guide", "https://example.com")));

        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> ContentTypeRules.validate(content));

        assertEquals("resources.type must be 'link' or 'file'", exception.getMessage());
    }

    @Test
    void shouldRejectMissingOrUnsafeSupplementalResourceFields() {
        final Content content = content(ContentType.TEXT, null, "Text", null);
        content.setResources(List.of(new ContentResource("1", ContentResource.TYPE_LINK, " ", "https://example.com")));
        assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(content));

        content.setResources(List.of(new ContentResource("1", ContentResource.TYPE_FILE, "Guide", " ")));
        assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(content));

        content.setResources(List.of(new ContentResource("1", ContentResource.TYPE_LINK, "Guide", "ftp://example.com")));
        assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(content));

        content.setResources(Arrays.asList((ContentResource) null));
        assertThrows(IllegalArgumentException.class, () -> ContentTypeRules.validate(content));
    }

    @Test
    void shouldAllowNullSupplementalResourceList() {
        final Content content = content(ContentType.TEXT, null, "Text", null);
        content.setResources(null);

        assertDoesNotThrow(() -> ContentTypeRules.validate(content));
    }

    private static Content content(ContentType type, String url, String description, Integer duration) {
        final Content content = new Content();
        content.setType(type);
        content.setUrl(url);
        content.setDescription(description);
        content.setDurationMinutes(duration);
        return content;
    }
}
