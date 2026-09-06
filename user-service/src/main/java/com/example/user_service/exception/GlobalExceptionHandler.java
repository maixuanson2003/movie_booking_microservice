package com.example.user_service.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.user_service.sharedLogic.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String EXCEPTION_HANDLED = "EXCEPTION_HANDLED";
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException ex, WebRequest request) {
        return error(ex.getMessage(), null, ex.getStatus(), new HttpHeaders(), request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(field -> errors.putIfAbsent(field.getField(), field.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(object -> errors.putIfAbsent(object.getObjectName(), object.getDefaultMessage()));
        return error("Validation failed", errors, status, headers, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        return error("Data conflicts with existing records or database constraints", null,
                HttpStatus.CONFLICT, new HttpHeaders(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        return error("Authentication required or invalid credentials", null,
                HttpStatus.UNAUTHORIZED, new HttpHeaders(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        return error("Access denied", null, HttpStatus.FORBIDDEN, new HttpHeaders(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(
            ResponseStatusException ex, WebRequest request) {
        return handleExceptionInternal(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        log.error("Unhandled request exception", ex);
        return error("Internal server error", null,
                HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders(), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String message = status.is5xxServerError() ? "Internal server error"
                : resolved != null ? resolved.getReasonPhrase() : "Request failed";
        return error(message, null, status, headers, request);
    }

    private ResponseEntity<Object> error(String message, Object data, HttpStatusCode status,
            HttpHeaders headers, WebRequest request) {
        request.setAttribute(EXCEPTION_HANDLED, true, WebRequest.SCOPE_REQUEST);
        return new ResponseEntity<>(new ApiResponse(message, data, false), headers, status);
    }
}
