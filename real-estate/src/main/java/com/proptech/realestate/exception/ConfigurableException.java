package com.proptech.realestate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Configurable Exception that uses message keys for internationalized error messages
 * Supports dynamic parameters and configurable stack trace display
 */
@Getter
public class ConfigurableException extends RuntimeException {
    
    private final String messageKey;
    private final Object[] messageArgs;
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final boolean includeStackTrace;
    
    /**
     * Create exception with message key only
     */
    public ConfigurableException(String messageKey) {
        this(messageKey, null, HttpStatus.INTERNAL_SERVER_ERROR, null, false, null);
    }
    
    /**
     * Create exception with message key and arguments
     */
    public ConfigurableException(String messageKey, Object[] messageArgs) {
        this(messageKey, messageArgs, HttpStatus.INTERNAL_SERVER_ERROR, null, false, null);
    }
    
    /**
     * Create exception with message key, arguments and HTTP status
     */
    public ConfigurableException(String messageKey, Object[] messageArgs, HttpStatus httpStatus) {
        this(messageKey, messageArgs, httpStatus, null, false, null);
    }
    
    /**
     * Create exception with message key, arguments, HTTP status and error code
     */
    public ConfigurableException(String messageKey, Object[] messageArgs, HttpStatus httpStatus, String errorCode) {
        this(messageKey, messageArgs, httpStatus, errorCode, false, null);
    }
    
    /**
     * Create exception with message key, arguments, HTTP status, error code and stack trace option
     */
    public ConfigurableException(String messageKey, Object[] messageArgs, HttpStatus httpStatus, 
                               String errorCode, boolean includeStackTrace) {
        this(messageKey, messageArgs, httpStatus, errorCode, includeStackTrace, null);
    }
    
    /**
     * Full constructor with cause
     */
    public ConfigurableException(String messageKey, Object[] messageArgs, HttpStatus httpStatus, 
                               String errorCode, boolean includeStackTrace, Throwable cause) {
        super(messageKey, cause); // Use messageKey as temporary message until resolution
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode != null ? errorCode : generateErrorCode(httpStatus);
        this.includeStackTrace = includeStackTrace;
    }
    
    /**
     * Generate error code from HTTP status if not provided
     */
    private String generateErrorCode(HttpStatus status) {
        return "ERR_" + status.value();
    }
    
    /**
     * Builder pattern for easier exception creation
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String messageKey;
        private Object[] messageArgs;
        private HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        private String errorCode;
        private boolean includeStackTrace = false;
        private Throwable cause;
        
        public Builder messageKey(String messageKey) {
            this.messageKey = messageKey;
            return this;
        }
        
        public Builder messageArgs(Object... messageArgs) {
            this.messageArgs = messageArgs;
            return this;
        }
        
        public Builder httpStatus(HttpStatus httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }
        
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public Builder includeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
            return this;
        }
        
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }
        
        public ConfigurableException build() {
            if (messageKey == null) {
                throw new IllegalArgumentException("Message key is required");
            }
            return new ConfigurableException(messageKey, messageArgs, httpStatus, errorCode, includeStackTrace, cause);
        }
    }
    
    /**
     * Static factory methods for common exceptions
     */
    public static ConfigurableException notFound(String resource, String field, Object value) {
        return builder()
            .messageKey("error.resource.not_found")
            .messageArgs(new Object[]{resource, field, value})
            .httpStatus(HttpStatus.NOT_FOUND)
            .errorCode("RESOURCE_NOT_FOUND")
            .build();
    }
    
    public static ConfigurableException badRequest(String messageKey, Object... args) {
        return builder()
            .messageKey(messageKey)
            .messageArgs(args)
            .httpStatus(HttpStatus.BAD_REQUEST)
            .errorCode("BAD_REQUEST")
            .build();
    }
    
    public static ConfigurableException unauthorized(String messageKey) {
        return builder()
            .messageKey(messageKey)
            .httpStatus(HttpStatus.UNAUTHORIZED)
            .errorCode("UNAUTHORIZED")
            .build();
    }
    
    public static ConfigurableException forbidden(String messageKey) {
        return builder()
            .messageKey(messageKey)
            .httpStatus(HttpStatus.FORBIDDEN)
            .errorCode("FORBIDDEN")
            .build();
    }
    
    public static ConfigurableException internalError(String messageKey, Throwable cause) {
        return builder()
            .messageKey(messageKey)
            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
            .errorCode("INTERNAL_SERVER_ERROR")
            .includeStackTrace(true)
            .cause(cause)
            .build();
    }
} 