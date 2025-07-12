package com.proptech.realestate.service;

import com.proptech.realestate.exception.ConfigurableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Demo service showcasing how to use ConfigurableException
 * with message keys, parameters, and configurable stack traces
 */
@Service
@Slf4j
public class DemoExceptionService {

    private final Random random = new Random();

    /**
     * Demo: Simple exception with message key only
     */
    public void throwSimpleException() {
        log.info("Demonstrating simple exception with message key");
        throw new ConfigurableException("error.unauthorized");
    }

    /**
     * Demo: Exception with message key and parameters
     */
    public void throwExceptionWithParameters(String propertyId) {
        log.info("Demonstrating exception with parameters for property: {}", propertyId);
        throw new ConfigurableException(
            "error.property.not_found", 
            new Object[]{propertyId}
        );
    }

    /**
     * Demo: Exception with custom HTTP status
     */
    public void throwExceptionWithCustomStatus(String email) {
        log.info("Demonstrating exception with custom HTTP status for email: {}", email);
        throw new ConfigurableException(
            "error.validation.invalid_email",
            new Object[]{email},
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }

    /**
     * Demo: Exception with builder pattern
     */
    public void throwExceptionWithBuilder(String resourceName, String fieldName, Object fieldValue) {
        log.info("Demonstrating exception with builder pattern");
        throw ConfigurableException.builder()
            .messageKey("error.resource.not_found")
            .messageArgs(new Object[]{resourceName, fieldName, fieldValue})
            .httpStatus(HttpStatus.NOT_FOUND)
            .errorCode("CUSTOM_NOT_FOUND")
            .includeStackTrace(true) // Force include stack trace for this exception
            .build();
    }

    /**
     * Demo: Static factory method - Not Found
     */
    public void throwNotFoundUsingFactory(String propertyId) {
        log.info("Demonstrating static factory method for not found");
        throw ConfigurableException.notFound("Property", "ID", propertyId);
    }

    /**
     * Demo: Static factory method - Bad Request
     */
    public void throwBadRequestUsingFactory(String invalidPrice) {
        log.info("Demonstrating static factory method for bad request");
        throw ConfigurableException.badRequest(
            "error.property.invalid_price", 
            invalidPrice
        );
    }

    /**
     * Demo: Static factory method - Unauthorized
     */
    public void throwUnauthorizedUsingFactory() {
        log.info("Demonstrating static factory method for unauthorized");
        throw ConfigurableException.unauthorized("error.user.invalid_credentials");
    }

    /**
     * Demo: Static factory method - Internal Error with cause
     */
    public void throwInternalErrorWithCause() {
        log.info("Demonstrating internal error with cause");
        try {
            // Simulate some operation that fails
            simulateFailingOperation();
        } catch (Exception cause) {
            throw ConfigurableException.internalError("error.database.connection_failed", cause);
        }
    }

    /**
     * Demo: Business logic validation with detailed error
     */
    public void validatePropertyPrice(BigDecimal price) {
        log.info("Validating property price: {}", price);
        
        if (price == null) {
            throw ConfigurableException.badRequest("error.validation.required_field_missing", "price");
        }
        
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw ConfigurableException.builder()
                .messageKey("error.validation.value_out_of_range")
                .messageArgs(new Object[]{"price", "0", "1000000000"})
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errorCode("INVALID_PRICE_RANGE")
                .build();
        }
        
        if (price.compareTo(new BigDecimal("1000000000")) > 0) {
            throw ConfigurableException.builder()
                .messageKey("error.validation.value_out_of_range")
                .messageArgs(new Object[]{"price", "0", "1000000000"})
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errorCode("PRICE_TOO_HIGH")
                .build();
        }
        
        log.info("Property price validation passed");
    }

    /**
     * Demo: MLS operation with different error scenarios
     */
    public void performMLSOperation(String feedId) {
        log.info("Performing MLS operation for feed: {}", feedId);
        
        if (feedId == null || feedId.trim().isEmpty()) {
            throw ConfigurableException.badRequest("error.validation.required_field_missing", "feedId");
        }
        
        // Simulate different error scenarios based on feedId
        switch (feedId.toLowerCase()) {
            case "not-found":
                throw ConfigurableException.notFound("MLS Feed", "ID", feedId);
                
            case "auth-failed":
                throw ConfigurableException.builder()
                    .messageKey("error.mls.authentication_failed")
                    .messageArgs(new Object[]{feedId})
                    .httpStatus(HttpStatus.UNAUTHORIZED)
                    .errorCode("MLS_AUTH_FAILED")
                    .build();
                    
            case "rate-limit":
                throw ConfigurableException.builder()
                    .messageKey("error.mls.rate_limit_exceeded")
                    .messageArgs(new Object[]{feedId})
                    .httpStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .errorCode("MLS_RATE_LIMIT")
                    .build();
                    
            case "data-error":
                throw ConfigurableException.builder()
                    .messageKey("error.mls.data_format_invalid")
                    .messageArgs(new Object[]{"Invalid JSON structure"})
                    .httpStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                    .errorCode("MLS_DATA_INVALID")
                    .includeStackTrace(true) // Include stack trace for debugging
                    .build();
                    
            case "system-error":
                // Simulate a system error with cause
                try {
                    simulateFailingOperation();
                } catch (Exception cause) {
                    throw ConfigurableException.builder()
                        .messageKey("error.mls.sync_failed")
                        .messageArgs(new Object[]{feedId, cause.getMessage()})
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .errorCode("MLS_SYNC_FAILED")
                        .includeStackTrace(true)
                        .cause(cause)
                        .build();
                }
                break;
                
            default:
                log.info("MLS operation completed successfully for feed: {}", feedId);
        }
    }

    /**
     * Demo: Search operation with Elasticsearch errors
     */
    public void performSearch(String query) {
        log.info("Performing search with query: {}", query);
        
        if (query == null || query.trim().isEmpty()) {
            throw ConfigurableException.badRequest("error.validation.required_field_missing", "query");
        }
        
        if (query.length() < 3) {
            throw ConfigurableException.builder()
                .messageKey("error.search.query_invalid")
                .messageArgs(new Object[]{"Query must be at least 3 characters long"})
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errorCode("QUERY_TOO_SHORT")
                .build();
        }
        
        // Simulate Elasticsearch unavailable
        if (query.contains("elasticsearch-down")) {
            throw ConfigurableException.builder()
                .messageKey("error.search.elasticsearch_unavailable")
                .httpStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .errorCode("SEARCH_SERVICE_UNAVAILABLE")
                .includeStackTrace(true)
                .build();
        }
        
        // Simulate too many results
        if (query.contains("*") || query.length() < 4) {
            throw ConfigurableException.builder()
                .messageKey("error.search.too_many_results")
                .httpStatus(HttpStatus.BAD_REQUEST)
                .errorCode("TOO_MANY_RESULTS")
                .build();
        }
        
        log.info("Search completed successfully");
    }

    /**
     * Demo: File upload with various validation scenarios
     */
    public void validateFileUpload(String fileName, long fileSize, String contentType) {
        log.info("Validating file upload: {} (size: {}, type: {})", fileName, fileSize, contentType);
        
        if (fileName == null || fileName.trim().isEmpty()) {
            throw ConfigurableException.badRequest("error.validation.required_field_missing", "fileName");
        }
        
        // Check file extension
        if (!fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|pdf|doc|docx)$")) {
            throw ConfigurableException.builder()
                .messageKey("error.file.invalid_format")
                .messageArgs(new Object[]{"jpg, jpeg, png, pdf, doc, docx"})
                .httpStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .errorCode("INVALID_FILE_FORMAT")
                .build();
        }
        
        // Check file size (10MB limit)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (fileSize > maxSize) {
            throw ConfigurableException.builder()
                .messageKey("error.file.size_exceeded")
                .messageArgs(new Object[]{"10MB"})
                .httpStatus(HttpStatus.PAYLOAD_TOO_LARGE)
                .errorCode("FILE_TOO_LARGE")
                .build();
        }
        
        log.info("File validation passed");
    }

    /**
     * Demo: Random exception scenario for testing
     */
    public void throwRandomException() {
        log.info("Throwing random exception for testing");
        
        int scenario = random.nextInt(5);
        
        switch (scenario) {
            case 0:
                throwSimpleException();
                break;
            case 1:
                throwExceptionWithParameters("PROP-" + random.nextInt(1000));
                break;
            case 2:
                throwNotFoundUsingFactory("USER-" + random.nextInt(1000));
                break;
            case 3:
                performMLSOperation("rate-limit");
                break;
            case 4:
                throwInternalErrorWithCause();
                break;
            default:
                log.info("No exception thrown in random scenario");
        }
    }

    /**
     * Simulate a failing operation for demonstration
     */
    private void simulateFailingOperation() {
        throw new RuntimeException("Simulated database connection failure");
    }
} 