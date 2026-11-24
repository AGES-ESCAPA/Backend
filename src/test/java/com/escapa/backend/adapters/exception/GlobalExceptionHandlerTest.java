package com.escapa.backend.adapters.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.escapa.backend.domain.certificate.CertificateGenerationException;
import com.escapa.backend.domain.certificate.CertificateGenerationUnavailableException;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.course.CourseValidationException;
import com.escapa.backend.domain.order.CourseAlreadyActiveException;
import com.escapa.backend.domain.order.OrderValidationException;
import com.escapa.backend.domain.order.SeatsInUseException;
import com.escapa.backend.domain.user.CannotDeactivateOwnAccountException;
import com.escapa.backend.domain.user.EmailChangeTokenExpiredException;
import com.escapa.backend.domain.user.EmailChangeTokenNotFoundException;
import com.escapa.backend.domain.user.InactiveUserException;
import com.escapa.backend.domain.user.InvalidAvatarFileException;
import com.escapa.backend.domain.user.InvalidCredentialsException;
import com.escapa.backend.domain.user.InvalidCurrentPasswordException;
import com.escapa.backend.domain.user.UserAlreadyInactiveException;

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

    @Test
    void shouldMapDomainExceptionsToExpectedHttpStatuses() {
        assertEquals(HttpStatus.BAD_REQUEST.value(),
                handler.handleInvalidCurrentPasswordException(new InvalidCurrentPasswordException(), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.NOT_FOUND.value(),
                handler.handleNotFoundException(new EmailChangeTokenNotFoundException(), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.FORBIDDEN.value(),
                handler.handleForbiddenAccountChange(new CannotDeactivateOwnAccountException(), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.CONFLICT.value(),
                handler.handleUserAlreadyInactiveException(
                        new UserAlreadyInactiveException(UUID.randomUUID()), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.GONE.value(),
                handler.handleGoneException(new EmailChangeTokenExpiredException(), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.UNAUTHORIZED.value(),
                handler.handleInvalidCredentialsException(new InvalidCredentialsException(), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.FORBIDDEN.value(),
                handler.handleInactiveUserException(new InactiveUserException(), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST.value(),
                handler.handleInvalidUploadedFileException(
                        InvalidAvatarFileException.unsupportedType("text/plain"), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.FORBIDDEN.value(),
                handler.handleAccessDeniedException(
                        new LessonAccessDeniedException(Reason.NOT_ENROLLED, UUID.randomUUID()), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(),
                handler.handleCertificateGenerationUnavailableException(
                        new CertificateGenerationUnavailableException("busy"), request)
                        .getStatusCode().value());
        assertEquals("Could not generate the certificate PDF",
                handler.handleCertificateGenerationException(
                        new CertificateGenerationException("chromium", null), request)
                        .getBody().message());
        assertEquals("Course incomplete: title, category",
                handler.handleCourseValidationException(
                        new CourseValidationException("Course incomplete", List.of("title", "category")), request)
                        .getBody().message());
    }

    @Test
    void shouldMapRequestParameterFailuresAndResponseStatusExceptions() {
        assertEquals("Invalid value for parameter 'page'",
                handler.handleTypeMismatchException(
                        new MethodArgumentTypeMismatchException(
                                "x", Integer.class, "page", null, new NumberFormatException()),
                        request)
                        .getBody().message());
        assertEquals("Missing required parameter 'file'",
                handler.handleMissingParameterException(
                        new MissingServletRequestParameterException("file", "MultipartFile"), request)
                        .getBody().message());
        assertEquals(HttpStatus.UNAUTHORIZED.value(),
                handler.handleResponseStatusException(
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid access token"),
                        request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST.value(),
                handler.handleIllegalArgumentException(new IllegalArgumentException("Name is required"), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.CONFLICT.value(),
                handler.handleOrderConflictException(new SeatsInUseException(3, 1), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST.value(),
                handler.handleOrderValidationException(
                        new OrderValidationException("paymentMethod CARD is required"), request)
                        .getStatusCode().value());
        assertEquals(HttpStatus.CONFLICT.value(),
                handler.handleCourseAlreadyActiveException(
                        new CourseAlreadyActiveException("Course already active"), request)
                        .getStatusCode().value());
    }
}
