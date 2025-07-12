package com.proptech.realestate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Unique error code for this type of error
     */
    private String errorCode;
    
    /**
     * Human-readable error message (localized)
     */
    private String message;
    
    /**
     * Additional error details (can be string, object, or list)
     */
    private Object details;
    
    /**
     * Timestamp when the error occurred
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Request ID for tracing purposes
     */
    private String requestId;
    
    /**
     * Request path where the error occurred
     */
    private String path;
    
    /**
     * HTTP status code
     */
    private Integer status;
    
    /**
     * Stack trace information (only included if configured)
     */
    private StackTraceInfo stackTrace;
    
    /**
     * Additional metadata about the error context
     */
    private Map<String, Object> metadata;
    
    /**
     * Validation errors (for form validation)
     */
    private List<ValidationError> validationErrors;
    
    /**
     * Nested error response for chained exceptions
     */
    private ErrorResponse cause;
    
    /**
     * Stack trace information structure
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StackTraceInfo {
        
        /**
         * Exception class name
         */
        private String exceptionClass;
        
        /**
         * File where exception occurred
         */
        private String fileName;
        
        /**
         * Line number where exception occurred
         */
        private Integer lineNumber;
        
        /**
         * Method name where exception occurred
         */
        private String methodName;
        
        /**
         * Full stack trace as string array
         */
        private List<String> stackTraceElements;
        
        /**
         * Simplified stack trace (only application code)
         */
        private List<String> applicationStackTrace;
    }
    
    /**
     * Validation error structure
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {
        
        /**
         * Field name that failed validation
         */
        private String field;
        
        /**
         * Rejected value
         */
        private Object rejectedValue;
        
        /**
         * Validation error message
         */
        private String message;
        
        /**
         * Validation error code
         */
        private String code;
    }
    
    /**
     * Static factory methods for common error responses
     */
    public static ErrorResponse of(String errorCode, String message) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .build();
    }
    
    public static ErrorResponse of(String errorCode, String message, Object details) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .details(details)
                .build();
    }
    
    public static ErrorResponse withStackTrace(String errorCode, String message, Throwable throwable) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .stackTrace(buildStackTraceInfo(throwable))
                .build();
    }
    
    public static ErrorResponse withValidationErrors(String errorCode, String message, 
                                                   List<ValidationError> validationErrors) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .validationErrors(validationErrors)
                .build();
    }
    
    /**
     * Build stack trace information from throwable
     */
    private static StackTraceInfo buildStackTraceInfo(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        StackTraceElement[] elements = throwable.getStackTrace();
        if (elements.length == 0) {
            return null;
        }
        
        // Get the first stack trace element (where exception occurred)
        StackTraceElement firstElement = elements[0];
        
        // Build full stack trace
        List<String> fullStackTrace = java.util.Arrays.stream(elements)
                .map(StackTraceElement::toString)
                .collect(java.util.stream.Collectors.toList());
        
        // Build application-only stack trace (filter framework code)
        List<String> appStackTrace = java.util.Arrays.stream(elements)
                .filter(element -> element.getClassName().startsWith("com.proptech.realestate"))
                .map(StackTraceElement::toString)
                .collect(java.util.stream.Collectors.toList());
        
        return StackTraceInfo.builder()
                .exceptionClass(throwable.getClass().getSimpleName())
                .fileName(firstElement.getFileName())
                .lineNumber(firstElement.getLineNumber())
                .methodName(firstElement.getMethodName())
                .stackTraceElements(fullStackTrace)
                .applicationStackTrace(appStackTrace)
                .build();
    }
    
    /**
     * Add metadata to the error response
     */
    public ErrorResponse addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Set request context information
     */
    public ErrorResponse withRequestContext(String requestId, String path, Integer status) {
        this.requestId = requestId;
        this.path = path;
        this.status = status;
        return this;
    }
}