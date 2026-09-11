package com.escapa.backend.common.api;

import com.escapa.backend.common.exception.BusinessException;
import com.escapa.backend.common.exception.BusinessRuleException;
import com.escapa.backend.common.exception.ConflictException;
import com.escapa.backend.common.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Único lugar da aplicação que traduz exceção em status HTTP.
 *
 * <p>Quatro famílias, na ordem em que aparecem abaixo:
 * <ol>
 *   <li><b>Validação de entrada</b> (400): Bean Validation, conversão de parâmetro, JSON malformado.</li>
 *   <li><b>Regra de negócio</b> (404 / 409 / 422): filhas de {@link BusinessException}, lançadas pelos services.</li>
 *   <li><b>Protocolo HTTP</b> (404 / 405 / 415): rota, método ou media type inexistentes.</li>
 *   <li><b>Infraestrutura</b> (409 / 500): banco e falhas inesperadas. O cliente nunca vê o detalhe;
 *       ele fica no log.</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    private static final String INTERNAL_ERROR_MESSAGE = "Unexpected error occurred";

    // ------------------------------------------------------------------
    // 1. Validação de entrada -> 400
    // ------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        final String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "'", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Missing required parameter '" + ex.getParameterName() + "'", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request body", request);
    }

    /**
     * Provisório: some no passo 2 do refactor, quando o último use case que lança
     * {@code IllegalArgumentException} para regra de negócio for substituído.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request);
    }

    // ------------------------------------------------------------------
    // 2. Regra de negócio -> 404 / 409 / 422
    // ------------------------------------------------------------------

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return business(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return business(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return business(HttpStatus.UNPROCESSABLE_ENTITY, ex, request);
    }

    private ResponseEntity<ApiError> business(HttpStatus status, BusinessException ex, HttpServletRequest request) {
        LOG.info("Business rule rejected {} {}: {} ({})",
                request.getMethod(), request.getRequestURI(), ex.getCode(), ex.getMessage());
        return build(status, ex.getCode(), ex.getMessage(), request);
    }

    // ------------------------------------------------------------------
    // 3. Protocolo HTTP -> 404 / 405 / 415
    // ------------------------------------------------------------------

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> handleNoResource(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Resource not found: " + request.getRequestURI(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Method " + ex.getMethod() + " not allowed for " + request.getRequestURI(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex,
                                                    HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Unsupported media type: " + ex.getContentType(), request);
    }

    // ------------------------------------------------------------------
    // 4. Infraestrutura -> 409 / 500 (detalhe só no log)
    // ------------------------------------------------------------------

    /**
     * Rede de segurança para condição de corrida: o service deve checar a regra
     * antes de gravar, então uma constraint violada aqui é exceção, não fluxo normal.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        LOG.warn("Data integrity violation on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT", "Request conflicts with existing data", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        LOG.error("Data access failure on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_CODE, INTERNAL_ERROR_MESSAGE, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        LOG.error("Unexpected failure on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_CODE, INTERNAL_ERROR_MESSAGE, request);
    }

    // ------------------------------------------------------------------

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           HttpServletRequest request) {
        final ApiError body = new ApiError(
                status.value(), status.getReasonPhrase(), code, message, request.getRequestURI(), Instant.now());
        return ResponseEntity.status(status).body(body);
    }
}
