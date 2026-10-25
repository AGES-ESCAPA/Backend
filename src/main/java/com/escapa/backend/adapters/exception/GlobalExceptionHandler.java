package com.escapa.backend.adapters.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.escapa.backend.domain.certificate.CertificateGenerationException;
import com.escapa.backend.domain.certificate.CertificateGenerationUnavailableException;
import com.escapa.backend.domain.certificate.CertificateAccessDeniedException;
import com.escapa.backend.domain.certificate.CertificateNotFoundException;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseValidationException;
import com.escapa.backend.domain.module.ModuleNotFoundException;
import com.escapa.backend.domain.order.OrderNotFoundException;
import com.escapa.backend.domain.user.AdminAccessDeniedException;
import com.escapa.backend.domain.user.CompanyEmployeeCannotDeactivateException;
import com.escapa.backend.domain.user.InactiveUserException;
import com.escapa.backend.domain.user.InvalidAvatarFileException;
import com.escapa.backend.domain.user.InvalidCredentialsException;
import com.escapa.backend.domain.user.InvalidCurrentPasswordException;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.user.ProfileNotEditableException;
import com.escapa.backend.domain.user.UserAlreadyInactiveException;
import com.escapa.backend.domain.user.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        final String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // 400, e nao 401: o frontend trata 401 como sessao expirada e deslogaria o usuario.
    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ApiError> handleInvalidCurrentPasswordException(
            InvalidCurrentPasswordException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            ContentNotFoundException.class,
            ModuleNotFoundException.class,
            CourseNotFoundException.class,
            CertificateNotFoundException.class,
            OrderNotFoundException.class
    })
    public ResponseEntity<ApiError> handleNotFoundException(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({
            CompanyEmployeeCannotDeactivateException.class,
            ProfileNotEditableException.class
    })
    public ResponseEntity<ApiError> handleForbiddenAccountChange(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ResponseEntity<ApiError> handleUserAlreadyInactiveException(
            UserAlreadyInactiveException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentialsException(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ApiError> handleInactiveUserException(
            InactiveUserException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidAvatarFileException.class)
    public ResponseEntity<ApiError> handleInvalidAvatarFileException(
            InvalidAvatarFileException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler({
            LessonAccessDeniedException.class,
            AdminAccessDeniedException.class,
            CertificateAccessDeniedException.class
    })
    public ResponseEntity<ApiError> handleAccessDeniedException(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(CertificateGenerationUnavailableException.class)
    public ResponseEntity<ApiError> handleCertificateGenerationUnavailableException(
            CertificateGenerationUnavailableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(CertificateGenerationException.class)
    public ResponseEntity<ApiError> handleCertificateGenerationException(
            CertificateGenerationException ex, HttpServletRequest request) {
        // A causa (Chromium, template) fica so no log; o cliente recebe uma mensagem generica.
        LOGGER.error("Certificate PDF generation failed for {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate the certificate PDF", request);
    }

    @ExceptionHandler(CourseValidationException.class)
    public ResponseEntity<ApiError> handleCourseValidationException(CourseValidationException ex, HttpServletRequest request) {
        final String msg = ex.getMessage() + ": " + String.join(", ", ex.getMissingFields());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, msg, request);
    }

    @ExceptionHandler(DuplicateRegistrationException.class)
    public ResponseEntity<ApiError> handleDuplicateRegistrationException(DuplicateRegistrationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request) {
        // Generico: users nao e mais a unica origem possivel de violacao de constraint.
        return buildResponse(HttpStatus.CONFLICT, "Resource already exists", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found: " + request.getRequestURI(), request);
    }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "File exceeds the maximum upload size allowed", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        LOGGER.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        if (status.is4xxClientError()) {
            LOGGER.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), status.value(), message);
        }
        final ApiError apiError = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(apiError);
    }
}
