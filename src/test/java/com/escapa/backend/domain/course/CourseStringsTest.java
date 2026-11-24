package com.escapa.backend.domain.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CourseStringsTest {

    @Test
    void shouldConvertNullAndBlankToNull() {
        assertNull(CourseStrings.blankToNull(null));
        assertNull(CourseStrings.blankToNull(""));
        assertNull(CourseStrings.blankToNull("   "));
        assertNull(CourseStrings.blankToNull("\t"));
    }

    @Test
    void shouldTrimNonBlankValues() {
        assertEquals("capa.png", CourseStrings.blankToNull("  capa.png  "));
        assertEquals("https://cdn.example.com/teaser.mp4",
                CourseStrings.blankToNull("https://cdn.example.com/teaser.mp4"));
    }
}
