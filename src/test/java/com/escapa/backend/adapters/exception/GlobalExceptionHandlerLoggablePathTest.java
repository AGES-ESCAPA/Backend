package com.escapa.backend.adapters.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerLoggablePathTest {

    private static String loggablePath(String uri) {
        return GlobalExceptionHandler.loggablePath(new MockHttpServletRequest("GET", uri));
    }

    @Test
    void shouldMaskTheVerificationCodeOfTheDownload() {
        assertEquals("/api/v1/certificates/ESC-***/download",
                loggablePath("/api/v1/certificates/ESC-2026-MKT-0001/download"));
    }

    @Test
    void shouldMaskTheVerificationCodeOfTheScreen() {
        assertEquals("/api/v1/certificates/ESC-***", loggablePath("/api/v1/certificates/ESC-2026-MKT-0001"));
    }

    @Test
    void shouldNeverShowMoreThanHalfOfAShortCode() {
        assertEquals("/api/v1/certificates/AB***", loggablePath("/api/v1/certificates/ABCDE"));
    }

    @Test
    void shouldKeepOtherPathsUnchanged() {
        assertEquals("/api/v1/student/enrollments", loggablePath("/api/v1/student/enrollments"));
    }
}
