package com.proptech.realestate.exception;

import com.proptech.realestate.dto.ApiResponse;
import com.proptech.realestate.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced Global Exception Handler with configurable stack traces,
 * internationalized messages, and comprehensive error logging
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // Configuration values for exception handling
    @Value("${app.exception.include-stack-trace.enabled:false}")
    private boolean globalStackTraceEnabled;

    @Value("${app.exception.include-stack-trace.include-in-dev-mode:true}")
    private boolean includeStackTraceInDev;

    @Value("${app.exception.detailed-errors.enabled:true}")
    private boolean detailedErrorsEnabled;

    @Value("${app.exception.detailed-errors.include-timestamp:true}")
    private boolean includeTimestamp;

    @Value("${app.exception.detailed-errors.include-request-id:true}")
    private boolean includeRequestId;

    @Value("${app.exception.detailed-errors.include-path:true}")
    private boolean includePath;

    @Value("${app.exception.logging.log-all-exceptions:true}")
    private boolean logAllExceptions;

    @Value("${app.exception.logging.include-stack-trace-in-logs:true}")
    private boolean includeStackTraceInLogs;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * Handle ConfigurableException - the main custom exception type
     */
    @ExceptionHandler(ConfigurableException.class)
    public ResponseEntity<ApiResponse<Object>> handleConfigurableException(
            ConfigurableException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Configurable exception occurred");
        
        // Resolve the localized message
        String localizedMessage = resolveMessage(ex.getMessageKey(), ex.getMessageArgs(), request.getLocale());
        
        // Determine if stack trace should be included
        boolean includeStackTrace = shouldIncludeStackTrace(ex);
        
        // Build error response
        ErrorResponse errorResponse = buildErrorResponse(
            ex.getErrorCode(), 
            localizedMessage, 
            ex, 
            request, 
            requestId, 
            includeStackTrace
        );
        
        // Create API response
        ApiResponse<Object> apiResponse = ApiResponse.error(localizedMessage, ex.getErrorCode(), errorResponse);
        
        return new ResponseEntity<>(apiResponse, ex.getHttpStatus());
    }

    /**
     * Handle ResourceNotFoundException (legacy support)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Resource not found");
        
        String message = resolveMessage("error.resource.not_found", 
            new Object[]{ex.getResourceName(), ex.getFieldName(), ex.getFieldValue()}, 
            request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "RESOURCE_NOT_FOUND", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "RESOURCE_NOT_FOUND", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle InvalidRequestException (legacy support)
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidRequestException(
            InvalidRequestException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Invalid request");
        
        String message = resolveMessage("error.bad_request", null, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "INVALID_REQUEST", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "INVALID_REQUEST", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle validation errors (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Validation error");
        
        String message = resolveMessage("error.validation.failed", null, request.getLocale());
        
        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::buildValidationError)
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
            "VALIDATION_ERROR", message, validationErrors)
            .withRequestContext(requestId, getRequestPath(request), HttpStatus.BAD_REQUEST.value());
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "VALIDATION_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle bind exceptions
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Object>> handleBindException(
            BindException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Bind exception");
        
        String message = resolveMessage("error.validation.failed", null, request.getLocale());
        
        List<ErrorResponse.ValidationError> validationErrors = ex.getFieldErrors()
            .stream()
            .map(this::buildValidationError)
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.withValidationErrors(
            "BIND_ERROR", message, validationErrors)
            .withRequestContext(requestId, getRequestPath(request), HttpStatus.BAD_REQUEST.value());
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "BIND_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Constraint violation");
        
        String message = resolveMessage("error.validation.constraint_violation", 
            new Object[]{ex.getMessage()}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "CONSTRAINT_VIOLATION", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "CONSTRAINT_VIOLATION", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle database access exceptions
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Database access error");
        
        String message = resolveMessage("error.database.query_failed", 
            new Object[]{ex.getMostSpecificCause().getMessage()}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "DATABASE_ERROR", message, ex, request, requestId, 
            shouldIncludeStackTraceForErrorCode("DATABASE_ERROR"));
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "DATABASE_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle data integrity violation exceptions
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Data integrity violation");
        
        String message = resolveMessage("error.database.data_integrity", 
            new Object[]{ex.getMostSpecificCause().getMessage()}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "DATA_INTEGRITY_ERROR", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "DATA_INTEGRITY_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Access denied");
        
        String message = resolveMessage("error.security.access_denied", 
            new Object[]{getRequestPath(request)}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "ACCESS_DENIED", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "ACCESS_DENIED", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Method argument type mismatch");
        
        String message = resolveMessage("error.validation.invalid_format", 
            new Object[]{ex.getName(), ex.getRequiredType().getSimpleName()}, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "INVALID_ARGUMENT_TYPE", message, ex, request, requestId, false);
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "INVALID_ARGUMENT_TYPE", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex, WebRequest request) {
        
        String requestId = generateRequestId();
        logException(ex, requestId, "Unhandled exception occurred");
        
        String message = resolveMessage("error.internal_server", null, request.getLocale());
        
        ErrorResponse errorResponse = buildErrorResponse(
            "INTERNAL_SERVER_ERROR", message, ex, request, requestId, 
            shouldIncludeStackTraceForErrorCode("INTERNAL_SERVER_ERROR"));
        
        ApiResponse<Object> apiResponse = ApiResponse.error(message, "INTERNAL_SERVER_ERROR", errorResponse);
        
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Build comprehensive error response
     */
    private ErrorResponse buildErrorResponse(String errorCode, String message, Throwable throwable, 
                                           WebRequest request, String requestId, boolean includeStackTrace) {
        
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message);
        
        // Add request context if enabled
        if (detailedErrorsEnabled) {
            if (includeRequestId) {
                builder.requestId(requestId);
            }
            if (includePath) {
                builder.path(getRequestPath(request));
            }
            if (includeTimestamp) {
                // Timestamp is included by default via @Builder.Default
            }
        }
        
        // Add stack trace if configured
        if (includeStackTrace && throwable != null) {
            builder.stackTrace(buildStackTraceInfo(throwable));
        }
        
        // Add cause if present
        if (throwable.getCause() != null) {
            builder.cause(buildErrorResponse(
                "CAUSED_BY", 
                throwable.getCause().getMessage(), 
                throwable.getCause(), 
                request, 
                requestId, 
                false // Don't include stack trace for causes
            ));
        }
        
        return builder.build();
    }

    /**
     * Build stack trace information
     */
    private ErrorResponse.StackTraceInfo buildStackTraceInfo(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        StackTraceElement[] elements = throwable.getStackTrace();
        if (elements.length == 0) {
            return null;
        }
        
        StackTraceElement firstElement = elements[0];
        
        List<String> fullStackTrace = java.util.Arrays.stream(elements)
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
        
        List<String> appStackTrace = java.util.Arrays.stream(elements)
                .filter(element -> element.getClassName().startsWith("com.proptech.realestate"))
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());
        
        return ErrorResponse.StackTraceInfo.builder()
                .exceptionClass(throwable.getClass().getSimpleName())
                .fileName(firstElement.getFileName())
                .lineNumber(firstElement.getLineNumber())
                .methodName(firstElement.getMethodName())
                .stackTraceElements(fullStackTrace)
                .applicationStackTrace(appStackTrace)
                .build();
    }

    /**
     * Build validation error from field error
     */
    private ErrorResponse.ValidationError buildValidationError(FieldError fieldError) {
        return ErrorResponse.ValidationError.builder()
            .field(fieldError.getField())
            .rejectedValue(fieldError.getRejectedValue())
            .message(fieldError.getDefaultMessage())
            .code(fieldError.getCode())
            .build();
    }

    /**
     * Resolve message from message source with fallback
     */
    private String resolveMessage(String messageKey, Object[] args, Locale locale) {
        try {
            return messageSource.getMessage(messageKey, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message key not found: {}", messageKey);
            return messageKey; // Fallback to key itself
        }
    }

    /**
     * Determine if stack trace should be included for ConfigurableException
     */
    private boolean shouldIncludeStackTrace(ConfigurableException ex) {
        // Exception-specific setting takes precedence
        if (ex.isIncludeStackTrace()) {
            return true;
        }
        
        // Check if it's configured for specific error codes
        if (shouldIncludeStackTraceForErrorCode(ex.getErrorCode())) {
            return true;
        }
        
        // Check development mode setting
        if (includeStackTraceInDev && isDevelopmentMode()) {
            return true;
        }
        
        // Global setting
        return globalStackTraceEnabled;
    }

    /**
     * Check if stack trace should be included for specific error code
     */
    private boolean shouldIncludeStackTraceForErrorCode(String errorCode) {
        // This could be made configurable via properties
        return errorCode != null && (
            errorCode.equals("INTERNAL_SERVER_ERROR") ||
            errorCode.equals("DATABASE_ERROR") ||
            errorCode.equals("EXTERNAL_SERVICE_ERROR")
        );
    }

    /**
     * Check if application is running in development mode
     */
    private boolean isDevelopmentMode() {
        return "dev".equals(activeProfile) || "development".equals(activeProfile) || "local".equals(activeProfile);
    }

    /**
     * Generate unique request ID for tracing
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get request path from WebRequest
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
            return httpRequest.getRequestURI();
        }
        return request.getDescription(false);
    }

    /**
     * Log exception with appropriate level and detail
     */
    private void logException(Throwable throwable, String requestId, String context) {
        if (!logAllExceptions) {
            return;
        }
        
        String logMessage = "{} [RequestID: {}]: {}";
        
        if (throwable instanceof ConfigurableException) {
            // Handled exceptions - use WARN level
            if (includeStackTraceInLogs) {
                log.warn(logMessage, context, requestId, throwable.getMessage(), throwable);
            } else {
                log.warn(logMessage, context, requestId, throwable.getMessage());
            }
        } else {
            // Unhandled exceptions - use ERROR level
            if (includeStackTraceInLogs) {
                log.error(logMessage, context, requestId, throwable.getMessage(), throwable);
            } else {
                log.error(logMessage, context, requestId, throwable.getMessage());
            }
        }
    }
}