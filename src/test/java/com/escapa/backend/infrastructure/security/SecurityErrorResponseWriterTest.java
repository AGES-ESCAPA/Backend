package com.escapa.backend.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityErrorResponseWriterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(objectMapper);

    @Test
    void shouldWriteApiErrorJsonBody() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/profile");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(request, response, HttpStatus.UNAUTHORIZED, "Missing or invalid access token");

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        final JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(401, body.get("status").asInt());
        assertEquals("Unauthorized", body.get("error").asText());
        assertEquals("Missing or invalid access token", body.get("message").asText());
        assertEquals("/api/v1/me/profile", body.get("path").asText());
        assertTrue(body.has("timestamp"));
    }
}
