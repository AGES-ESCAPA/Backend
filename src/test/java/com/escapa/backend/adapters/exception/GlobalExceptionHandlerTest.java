package com.escapa.backend.adapters.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users");

    @Test
    void shouldDistinguishMalformedBodiesWithAndWithoutFieldInformation() {
        final HttpMessageNotReadableException generic = new HttpMessageNotReadableException(
                "Malformed", new IllegalArgumentException("bad json"));
        final HttpMessageNotReadableException invalidFormat = new HttpMessageNotReadableException(
                "Invalid field", InvalidFormatException.from(null, "bad value", "?", String.class));
        final InvalidFormatException withField = InvalidFormatException.from(
                null, "bad value", "?", String.class);
        withField.prependPath(GlobalExceptionHandlerTest.class, "userType");

        assertEquals("Malformed request body", handler.handleUnreadableBody(generic, request).getBody().message());
        assertEquals("Malformed request body",
                handler.handleUnreadableBody(invalidFormat, request).getBody().message());
        assertEquals("Invalid value for field 'userType'",
                handler.handleUnreadableBody(
                        new HttpMessageNotReadableException("Invalid field", withField), request)
                        .getBody().message());
    }

    @Test
    void shouldTranslatePersistenceDuplicateAndKeepUnexpectedFailuresGeneric() {
        final ConstraintViolationException violation = new ConstraintViolationException(
                "Duplicate", new SQLException("Duplicate", "23505"), "users_email_key");
        final TransactionSystemException duplicate = new TransactionSystemException("Commit failed", violation);
        final TransactionSystemException unexpected =
                new TransactionSystemException("Commit failed", new IllegalStateException("unknown"));

        assertEquals(HttpStatus.CONFLICT.value(),
                handler.handlePersistenceFailure(duplicate, request).getStatusCode().value());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                handler.handlePersistenceFailure(unexpected, request).getStatusCode().value());
    }

    @Test
    void shouldTranslateOnlyKnownDataIntegrityConflicts() {
        final DataIntegrityViolationException duplicate = new DataIntegrityViolationException(
                "Duplicate", new ConstraintViolationException(
                        "Duplicate", new SQLException("Duplicate", "23505"), "regular_users_cpf_key"));
        final DataIntegrityViolationException unexpected =
                new DataIntegrityViolationException("Unknown constraint");

        assertEquals("cpf already registered",
                handler.handleDataIntegrityViolationException(duplicate, request).getBody().message());
        assertEquals("Resource already exists",
                handler.handleDataIntegrityViolationException(unexpected, request).getBody().message());
    }

    @Test
    void shouldRejectUploadsLargerThanTheLimit() {
        final var response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(1024L), request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertEquals("File exceeds the maximum upload size allowed", response.getBody().message());
    }

    @Test
    void shouldReportUnknownPathsAsNotFound() {
        final var response = handler.handleNoResourceFoundException(
                new NoResourceFoundException(HttpMethod.GET, request.getRequestURI()), request);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertEquals("Resource not found: /api/v1/users", response.getBody().message());
    }

    @Test
    void shouldHideUnexpectedErrorsBehindAGenericResponse() {
        final var response = handler.handleGenericException(new IllegalStateException("db down"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertEquals("Unexpected error occurred", response.getBody().message());
    }
}
